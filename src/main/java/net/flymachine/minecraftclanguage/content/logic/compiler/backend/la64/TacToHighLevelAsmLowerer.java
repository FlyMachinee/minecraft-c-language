package net.flymachine.minecraftclanguage.content.logic.compiler.backend.la64;

import net.flymachine.minecraftclanguage.content.logic.architecture.la64.isa.instruction.LA64FloatCompareCondition;
import net.flymachine.minecraftclanguage.content.logic.architecture.la64.register.FloatingPointRegister;
import net.flymachine.minecraftclanguage.content.logic.architecture.la64.register.GeneralPurposeRegister;
import net.flymachine.minecraftclanguage.content.logic.architecture.la64.util.BitMath;
import net.flymachine.minecraftclanguage.content.logic.assembler.la64.LA64Assembler;
import net.flymachine.minecraftclanguage.content.logic.compiler.backend.la64.highLevel.*;
import net.flymachine.minecraftclanguage.content.logic.compiler.backend.la64.highLevel.instruction.*;
import net.flymachine.minecraftclanguage.content.logic.compiler.backend.la64.highLevel.operand.*;
import net.flymachine.minecraftclanguage.content.logic.compiler.common.BinaryOperator;
import net.flymachine.minecraftclanguage.content.logic.compiler.common.Comparison;
import net.flymachine.minecraftclanguage.content.logic.compiler.common.constant.Constant;
import net.flymachine.minecraftclanguage.content.logic.compiler.common.constant.ConstantDouble;
import net.flymachine.minecraftclanguage.content.logic.compiler.common.staticInit.DoubleInit;
import net.flymachine.minecraftclanguage.content.logic.compiler.common.type.BasicType;
import net.flymachine.minecraftclanguage.content.logic.compiler.common.type.PointerType;
import net.flymachine.minecraftclanguage.content.logic.compiler.common.type.Type;
import net.flymachine.minecraftclanguage.content.logic.compiler.frontend.c99.SymbolTable;
import net.flymachine.minecraftclanguage.content.logic.compiler.ir.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static net.flymachine.minecraftclanguage.content.logic.architecture.la64.isa.instruction.LA64FloatCompareCondition.*;
import static net.flymachine.minecraftclanguage.content.logic.architecture.la64.register.ConditionFlagRegister.FCC0;
import static net.flymachine.minecraftclanguage.content.logic.architecture.la64.register.FloatingPointRegister.*;
import static net.flymachine.minecraftclanguage.content.logic.architecture.la64.register.GeneralPurposeRegister.*;

public final class TacToHighLevelAsmLowerer implements TacVisitor<Void> {

    public TacToHighLevelAsmLowerer(SymbolTable symbolTable) {
        this.symbolTable = symbolTable;
    }

    private final SymbolTable symbolTable;
    private final BackendSymbolTable backendSymbolTable = new BackendSymbolTable();

    public BackendSymbolTable getBackendSymbolTable() {
        return backendSymbolTable;
    }

    public HighLevelProgram lower(TacProgram tacProgram) {
        List<HighLevelTopLevel> topLevels = new ArrayList<>();
        for (TacTopLevel topLevel : tacProgram.topLevels) {
            if (topLevel instanceof TacFunction func) {
                topLevels.add(lowerFunction(func));
            } else if (topLevel instanceof TacStaticVariable staticVar) {
                topLevels.add(
                    new HighLevelStaticVar(staticVar.name, staticVar.global, staticVar.type.alignof(), staticVar.init));
            } else if (topLevel instanceof TacStaticConstant staticConst) {
                topLevels.add(new HighLevelStaticConst(staticConst.name, staticConst.type.alignof(), staticConst.init));
            }
        }

        // 建立后端符号表
        for (SymbolTable.Entry entry : symbolTable.getEntries()) {
            String name = entry.id.name;
            if (entry.attr instanceof SymbolTable.Entry.FuncAttr) {
                backendSymbolTable.put(name, new BackendSymbolTable.FuncEntry(entry.attr.isDefinition()));
            } else if (entry.attr instanceof SymbolTable.Entry.ConstantAttr) {
                AsmType asmType = entry.type.toAsmType();
                backendSymbolTable.put(name, new BackendSymbolTable.ObjectEntry(asmType, true, true));
            } else {
                AsmType asmType = entry.type.toAsmType();
                boolean isStatic = entry.attr instanceof SymbolTable.Entry.StaticAttr;
                backendSymbolTable.put(name, new BackendSymbolTable.ObjectEntry(asmType, isStatic, false));
            }
        }

        // 处理浮点常量池
        for (Map.Entry<Double, String> entry : doubleConstants.entrySet()) {
            double value = entry.getKey();
            String label = entry.getValue();
            backendSymbolTable.put(label, new BackendSymbolTable.ObjectEntry(AsmType.DOUBLE, true, true));
            topLevels.add(new HighLevelStaticConst(label, 8, new DoubleInit(value)));
        }
        return new HighLevelProgram(topLevels);
    }

    private List<HighLevelInstruction> target;

    private static final GeneralPurposeRegister[] GPR_ARGS = {A0, A1, A2, A3, A4, A5, A6, A7};
    private static final FloatingPointRegister[] FPR_ARGS = {FA0, FA1, FA2, FA3, FA4, FA5, FA6, FA7};

    private int maxCallStackArgSize;

    private Immediate immediate(Constant constant) {
        return new Immediate(constant.toByteRepresentation());
    }

    private int labelCounter = 0;

    private String makeBackendLabel(String prefix) {
        return prefix + "_backend" + labelCounter++;
    }

    // 记录传递的参数存放的位置
    private record ArgumentPassingInfo(List<Integer> gprArgs, List<Integer> fprArgs, List<Integer> stackArgs) { }

    private ArgumentPassingInfo getArgumentPassingInfo(List<AsmType> asmTypes) {
        List<Integer> gprArgs = new ArrayList<>();
        List<Integer> fprArgs = new ArrayList<>();
        List<Integer> stackArgs = new ArrayList<>();

        int index = 0;
        for (AsmType asmType : asmTypes) {
            if (asmType.equals(AsmType.DOUBLE)) {
                if (fprArgs.size() < FPR_ARGS.length) {
                    fprArgs.add(index);
                } else if (gprArgs.size() < GPR_ARGS.length) {
                    gprArgs.add(index);
                } else {
                    stackArgs.add(index);
                }
            } else {
                if (gprArgs.size() < GPR_ARGS.length) {
                    gprArgs.add(index);
                } else {
                    stackArgs.add(index);
                }
            }
            index++;
        }
        return new ArgumentPassingInfo(gprArgs, fprArgs, stackArgs);
    }

    private HighLevelFunction lowerFunction(TacFunction tacFunction) {
        target = new ArrayList<>();
        maxCallStackArgSize = 0;

        // 拷贝参数至栈上
        List<AsmType> asmTypes = tacFunction.params.stream()
                                                   .map(name -> symbolTable.get(name).type.toAsmType())
                                                   .toList();
        ArgumentPassingInfo argPassingInfo = getArgumentPassingInfo(asmTypes);

        int gprIndex = 0;
        for (int argIndex : argPassingInfo.gprArgs) {
            String name = tacFunction.params.get(argIndex);
            AsmType asmType = asmTypes.get(argIndex);
            target.add(new Move(asmType, GPR_ARGS[gprIndex], new Pseudo(name)));
            gprIndex++;
        }

        int fprIndex = 0;
        for (int argIndex : argPassingInfo.fprArgs) {
            String name = tacFunction.params.get(argIndex);
            target.add(new Move(AsmType.DOUBLE, FPR_ARGS[fprIndex], new Pseudo(name)));
            fprIndex++;
        }

        int stackOffset = 0;
        for (int argIndex : argPassingInfo.stackArgs) {
            String name = tacFunction.params.get(argIndex);
            AsmType asmType = asmTypes.get(argIndex);
            target.add(new Move(asmType, new Memory(FP, stackOffset), new Pseudo(name)));
            stackOffset += 8;
        }

        List<TacInstruction> instructions = tacFunction.insts;
        for (TacInstruction instruction : instructions) {
            instruction.accept(this);
        }
        HighLevelFunction function = new HighLevelFunction(tacFunction.name, tacFunction.global, target);
        function.maxCallStackArgSize = maxCallStackArgSize;
        return function;
    }

    @Override
    public Void visit(TacReturn inst) {
        HighLevelOperand src = lowerValue(inst.value);
        AsmType asmType = getType(inst.value).toAsmType();
        if (asmType.equals(AsmType.DOUBLE)) {
            target.add(new Move(AsmType.DOUBLE, src, FA0));
        } else {
            target.add(new Move(asmType, src, A0));
        }
        target.add(new Ret());
        return null;
    }

    private void checkCharacterType(TacValue value) {
        if (getType(value).isCharacter()) {
            throw new IllegalStateException("Character type should be promoted to int before any arithmetic operation");
        }
    }

    @Override
    public Void visit(TacUnaryOperation inst) {
        checkCharacterType(inst.src);
        AsmType asmType = getType(inst.src).toAsmType();
        HighLevelOperand src = lowerValue(inst.src);
        HighLevelOperand dst = lowerValue(inst.dst);

        switch (inst.op) {
            case POSITIVE -> throw new UnsupportedOperationException("Positive operator should not appear in TAC");
            case NEGATE -> {
                if (asmType.equals(AsmType.DOUBLE)) {
                    // fneg.d
                    target.add(new DoubleNegate(src, dst));
                    return null;
                }

                // sub.w(d) dst r0 src
                var op = Binary.Operator.SUB;
                target.add(new Binary(op, asmType, ZERO, src, dst));
            }
            case COMPLEMENT -> {
                // nor dst src r0
                var op = Bitwise.Operator.NOR;
                target.add(new Bitwise(op, asmType, src, ZERO, dst));
            }
            case NOT -> {
                if (asmType.equals(AsmType.DOUBLE)) {
                    target.add(new Move(AsmType.DOUBLE, ZERO, FT1));
                    target.add(new CompareDouble(CEQ, src, FT1, FCC0));
                    target.add(new GetCC(FCC0, dst));
                    return null;
                }

                // sltui dst src 1
                target.add(new Compare(Comparison.LESS, true, asmType, src, new Immediate(1), dst));
            }
        }
        return null;
    }

    @Override
    public Void visit(TacBinaryOperation inst) {
        checkCharacterType(inst.lhs);
        checkCharacterType(inst.rhs);
        Type lhsType = getType(inst.lhs);
        HighLevelOperand lhs = lowerValue(inst.lhs);
        HighLevelOperand rhs = lowerValue(inst.rhs);
        HighLevelOperand dst = lowerValue(inst.dst);
        AsmType asmType = lhsType.toAsmType();

        boolean isUnsigned = false;
        if (lhsType instanceof BasicType bt && bt.isUnsigned()) {
            isUnsigned = true;
        } else if (lhsType instanceof PointerType) {
            isUnsigned = true;
        }

        switch (inst.op) {
            case ADD, SUBTRACT, MULTIPLY -> target.add(new Binary(inst.op, asmType, lhs, rhs, dst));
            case BITWISE_AND, BITWISE_OR, BITWISE_XOR -> target.add(new Bitwise(inst.op, asmType, lhs, rhs, dst));
            case DIVIDE, MODULO ->
                target.add(new DivOrMod(inst.op == BinaryOperator.DIVIDE, asmType, isUnsigned, lhs, rhs, dst));
            case LEFT_SHIFT, RIGHT_SHIFT -> {
                boolean isLeftShift = inst.op == BinaryOperator.LEFT_SHIFT;
                target.add(new BitwiseShift(isLeftShift, asmType, isUnsigned, lhs, rhs, dst));
            }
            case LOGICAL_AND, LOGICAL_OR ->
                throw new UnsupportedOperationException("Logical operators should be lowered to branches");
            case LESS_THAN, GREATER_THAN, LESS_OR_EQUAL, GREATER_OR_EQUAL, EQUAL, NOT_EQUAL -> {
                Comparison cmp = inst.op.toComparison();
                if (asmType.equals(AsmType.DOUBLE)) {
                    boolean swap = false;
                    LA64FloatCompareCondition cond = switch (cmp) {
                        case EQUAL -> CEQ;
                        case NOT_EQUAL -> CUNE;
                        case LESS -> CLT;
                        case GREATER -> {
                            swap = true;
                            yield CLT;
                        }
                        case LESS_EQUAL -> CLE;
                        case GREATER_EQUAL -> {
                            swap = true;
                            yield CLE;
                        }
                    };
                    target.add(new CompareDouble(cond, swap ? rhs : lhs, swap ? lhs : rhs, FCC0));
                    target.add(new GetCC(FCC0, dst));
                    return null;
                }
                target.add(new Compare(cmp, isUnsigned, asmType, lhs, rhs, dst));
                return null;
            }
            default -> throw new UnsupportedOperationException("Unsupported binary operator: " + inst.op);
        }
        return null;
    }

    @Override
    public Void visit(TacCopy inst) {
        HighLevelOperand src = lowerValue(inst.src);
        HighLevelOperand dst = lowerValue(inst.dst);
        AsmType srcType = getType(inst.src).toAsmType();
        AsmType dstType = getType(inst.dst).toAsmType();

        // TacCopy 表示将 src 的值复制到 dst，它们的类型可能不同，但二进制表示相同（在其宽度下）
        // 但 HLAsm 中的 Move 指令仅表示移动相同二进制表示的值，并且需要考虑它们在寄存器中的存放形态
        // 如 (signed) char 拷贝至 unsigned char，二者都是 8 位，所以使用 TacCopy
        // 但存放在寄存器中时，(signed) char 是符号拓展的，而 unsigned char 是零拓展的，此时就不能使用 Move 指令，而是相应的拓展指令了

        if (srcType.equals(dstType)) {
            target.add(new Move(srcType, src, dst));
        } else {
            if (!dstType.equals(AsmType.BYTE) && !dstType.equals(AsmType.UBYTE)) {
                throw new IllegalStateException("control should not reach here, dstType: " + dstType);
            }

            if (dstType.equals(AsmType.BYTE)) {
                target.add(new SignExtend(srcType, dstType, src, dst));
            } else {
                target.add(new ZeroExtend(srcType, dstType, src, dst));
            }
        }
        return null;
    }

    @Override
    public Void visit(TacLabel inst) {
        target.add(new Label(inst.name));
        return null;
    }

    @Override
    public Void visit(TacJump inst) {
        target.add(new Branch(inst.target));
        return null;
    }

    @Override
    public Void visit(TacJumpIfZero inst) {
        if (inst.cond instanceof TacConstant tacConstant) {
            // 常量检查
            if (tacConstant.value.isZero()) {
                target.add(new Branch(inst.target));
            }
            return null;
        }

        AsmType asmType = getType(inst.cond).toAsmType();
        HighLevelOperand cond = lowerValue(inst.cond);
        if (asmType.equals(AsmType.DOUBLE)) {
            target.add(new Move(AsmType.DOUBLE, ZERO, FT1));
            target.add(new CompareDouble(CEQ, cond, FT1, FCC0));
            target.add(new BranchIfCCNotZero(FCC0, inst.target));
            return null;
        }

        target.add(new BranchIfZero(asmType, cond, inst.target));
        return null;
    }

    @Override
    public Void visit(TacJumpIfNotZero inst) {
        if (inst.cond instanceof TacConstant tacConstant) {
            // 常量检查
            if (!tacConstant.value.isZero()) {
                target.add(new Branch(inst.target));
            }
            return null;
        }

        AsmType asmType = getType(inst.cond).toAsmType();
        HighLevelOperand cond = lowerValue(inst.cond);
        if (asmType.equals(AsmType.DOUBLE)) {
            target.add(new Move(AsmType.DOUBLE, ZERO, FT1));
            target.add(new CompareDouble(CUNE, cond, FT1, FCC0));
            target.add(new BranchIfCCNotZero(FCC0, inst.target));
            return null;
        }

        target.add(new BranchIfNotZero(asmType, cond, inst.target));
        return null;
    }

    @Override
    public Void visit(TacJumpIfComparison inst) {
        checkCharacterType(inst.lhs);
        checkCharacterType(inst.rhs);

        if (inst.lhs instanceof TacConstant lhsConstant && inst.rhs instanceof TacConstant rhsConstant) {
            // 常量检查
            boolean conditionMet = !lhsConstant.value.apply(inst.cond, rhsConstant.value).isZero();
            if (conditionMet ^ inst.inverse) {
                target.add(new Branch(inst.target));
            }
            return null;
        }

        Type type = getType(inst.lhs);
        AsmType asmType = type.toAsmType();
        HighLevelOperand lhs = lowerValue(inst.lhs);
        HighLevelOperand rhs = lowerValue(inst.rhs);

        if (asmType.equals(AsmType.DOUBLE)) {
            boolean swap = false;
            LA64FloatCompareCondition cond = switch (inst.cond) {
                case EQUAL -> CEQ;
                case NOT_EQUAL -> CUNE;
                case LESS -> CLT;
                case GREATER -> {
                    swap = true;
                    yield CLT;
                }
                case LESS_EQUAL -> CLE;
                case GREATER_EQUAL -> {
                    swap = true;
                    yield CLE;
                }
            };
            target.add(new CompareDouble(cond, swap ? rhs : lhs, swap ? lhs : rhs, FCC0));
            if (inst.inverse) {
                target.add(new BranchIfCCZero(FCC0, inst.target));
            } else {
                target.add(new BranchIfCCNotZero(FCC0, inst.target));
            }
            return null;
        }

        boolean isUnsigned = false;
        if (type instanceof BasicType bt && bt.isUnsigned()) {
            isUnsigned = true;
        } else if (type instanceof PointerType) {
            isUnsigned = true;
        }
        Comparison cmp = switch (inst.cond) {
            case EQUAL -> inst.inverse ? Comparison.NOT_EQUAL : Comparison.EQUAL;
            case NOT_EQUAL -> inst.inverse ? Comparison.EQUAL : Comparison.NOT_EQUAL;
            case LESS -> inst.inverse ? Comparison.GREATER_EQUAL : Comparison.LESS;
            case GREATER_EQUAL -> inst.inverse ? Comparison.LESS : Comparison.GREATER_EQUAL;
            case GREATER -> inst.inverse ? Comparison.LESS_EQUAL : Comparison.GREATER;
            case LESS_EQUAL -> inst.inverse ? Comparison.GREATER : Comparison.LESS_EQUAL;
        };
        target.add(new BranchIfComparison(cmp, isUnsigned, asmType, lhs, rhs, inst.target));
        return null;
    }

    private void passArguments(List<TacValue> args) {
        // 参数传递逻辑
        // 不需要分配或回收栈空间，由函数序言和尾声进行处理，函数序言中会分配好足够使用的栈空间
        List<AsmType> asmTypes = args.stream()
                                     .map(arg -> getType(arg).toAsmType())
                                     .toList();
        ArgumentPassingInfo argPassingInfo = getArgumentPassingInfo(asmTypes);

        int gprIndex = 0;
        for (int argIndex : argPassingInfo.gprArgs) {
            HighLevelOperand arg = lowerValue(args.get(argIndex));
            AsmType asmType = asmTypes.get(argIndex);
            target.add(new Move(asmType, arg, GPR_ARGS[gprIndex]));
            gprIndex++;
        }

        int fprIndex = 0;
        for (int argIndex : argPassingInfo.fprArgs) {
            HighLevelOperand arg = lowerValue(args.get(argIndex));
            target.add(new Move(AsmType.DOUBLE, arg, FPR_ARGS[fprIndex]));
            fprIndex++;
        }

        int stackOffset = 0;
        for (int argIndex : argPassingInfo.stackArgs) {
            HighLevelOperand arg = lowerValue(args.get(argIndex));
            AsmType asmType = asmTypes.get(argIndex);
            target.add(new Move(asmType, arg, new Memory(SP, stackOffset)));
            stackOffset += 8;
        }

        maxCallStackArgSize = Math.max(maxCallStackArgSize, stackOffset);
    }

    private void getReturnValue(TacValue dst) {
        HighLevelOperand dstOp = lowerValue(dst);
        AsmType asmType = getType(dst).toAsmType();
        if (asmType.equals(AsmType.DOUBLE)) {
            target.add(new Move(AsmType.DOUBLE, FA0, dstOp));
        } else {
            target.add(new Move(asmType, A0, dstOp));
        }
    }

    @Override
    public Void visit(TacDirectCall inst) {
        passArguments(inst.args);
        target.add(new Call(inst.funcDesignator));
        getReturnValue(inst.dst);
        return null;
    }

    @Override
    public Void visit(TacSignExtend inst) {
        AsmType srcType = getType(inst.src).toAsmType();
        AsmType dstType = getType(inst.dst).toAsmType();
        HighLevelOperand src = lowerValue(inst.src);
        HighLevelOperand dst = lowerValue(inst.dst);
        target.add(new SignExtend(srcType, dstType, src, dst));
        return null;
    }

    @Override
    public Void visit(TacTruncate inst) {
        HighLevelOperand src = lowerValue(inst.src);
        HighLevelOperand dst = lowerValue(inst.dst);
        BasicType targetType = (BasicType) getType(inst.dst);
        AsmType srcType = getType(inst.src).toAsmType();
        AsmType dstType = targetType.toAsmType();
        switch (targetType.primitive()) {
            case CHAR, SIGNED_CHAR, INT, UNSIGNED_INT -> target.add(new SignExtend(srcType, dstType, src, dst));
            case UNSIGNED_CHAR -> target.add(new ZeroExtend(srcType, dstType, src, dst));
            case LONG, UNSIGNED_LONG, DOUBLE ->
                throw new IllegalStateException("invalid Truncate to long, unsigned long or double");
        }
        return null;
    }

    @Override
    public Void visit(TacZeroExtend inst) {
        AsmType srcType = getType(inst.src).toAsmType();
        AsmType dstType = getType(inst.dst).toAsmType();
        HighLevelOperand src = lowerValue(inst.src);
        HighLevelOperand dst = lowerValue(inst.dst);
        target.add(new ZeroExtend(srcType, dstType, src, dst));
        return null;
    }

    @Override
    public Void visit(TacDoubleToInt inst) {
        HighLevelOperand src = lowerValue(inst.src);
        HighLevelOperand dst = lowerValue(inst.dst);
        AsmType asmType = getType(inst.dst).toAsmType();
        if (asmType.equals(AsmType.BYTE)) {
            // double -> int -> (signed) char
            target.add(new DoubleToIntRoundZero(src, T0, AsmType.WORD));
            target.add(new SignExtend(AsmType.WORD, AsmType.BYTE, T0, dst));
        } else {
            target.add(new DoubleToIntRoundZero(src, dst, asmType));
        }
        return null;
    }

    @Override
    public Void visit(TacDoubleToUnsignedInt inst) {
        HighLevelOperand src = lowerValue(inst.src);
        HighLevelOperand dst = lowerValue(inst.dst);
        AsmType asmType = getType(inst.dst).toAsmType();

        if (asmType.equals(AsmType.UBYTE)) {
            // double -> int -> unsigned char
            target.add(new DoubleToIntRoundZero(src, T0, AsmType.WORD));
            target.add(new ZeroExtend(AsmType.WORD, AsmType.UBYTE, T0, dst));
        } else if (asmType.equals(AsmType.WORD)) {
            // double -> long -> unsigned int
            target.add(new DoubleToIntRoundZero(src, T0, AsmType.DWORD));
            target.add(new SignExtend(AsmType.DWORD, AsmType.WORD, T0, dst));
        } else if (asmType.equals(AsmType.DWORD)) {
            // double -> unsigned long
            String labelInRange = makeBackendLabel("in_range");
            String labelEnd = makeBackendLabel("end_of_d2ul");

            // 和 2^63 比较，检查是否在 long 访问内
            // 0x43E0000000000000L 为 2^63 的浮点数位表示
            target.add(new Move(AsmType.DOUBLE, new Immediate(0x43E0000000000000L), FT1));
            target.add(new CompareDouble(CLT, src, FT1, FCC0));
            target.add(new BranchIfCCNotZero(FCC0, labelInRange));

            // 大于等于 2^63，超出 long，先减去 2^63 再加回来
            target.add(new Binary(BinaryOperator.SUBTRACT, AsmType.DOUBLE, src, FT1, FT0));
            target.add(new DoubleToIntRoundZero(FT0, dst, AsmType.DWORD));
            target.add(new Bitwise(BinaryOperator.BITWISE_OR, AsmType.DWORD, dst, new Immediate(1L << 63), dst));
            target.add(new Branch(labelEnd));

            // 小于 2^63，在 long 范围内，直接转换
            target.add(new Label(labelInRange));
            target.add(new DoubleToIntRoundZero(src, dst, AsmType.DWORD));

            target.add(new Label(labelEnd));
        } else {
            throw new IllegalStateException("Unrecognized asm type: " + asmType);
        }
        return null;
    }

    @Override
    public Void visit(TacIntToDouble inst) {
        HighLevelOperand src = lowerValue(inst.src);
        HighLevelOperand dst = lowerValue(inst.dst);
        AsmType asmType = getType(inst.src).toAsmType();
        if (asmType.equals(AsmType.BYTE)) {
            // (signed) char -> int -> double
            target.add(new SignExtend(AsmType.BYTE, AsmType.WORD, src, T0));
            target.add(new DoubleFromInt(T0, dst, AsmType.WORD));
        } else {
            target.add(new DoubleFromInt(src, dst, asmType));
        }
        return null;
    }

    @Override
    public Void visit(TacUnsignedIntToDouble inst) {
        HighLevelOperand src = lowerValue(inst.src);
        HighLevelOperand dst = lowerValue(inst.dst);
        AsmType srcAsmType = getType(inst.src).toAsmType();

        if (srcAsmType.equals(AsmType.UBYTE)) {
            // unsigned char -> int -> double
            target.add(new ZeroExtend(AsmType.UBYTE, AsmType.WORD, src, T0));
            target.add(new DoubleFromInt(T0, dst, AsmType.WORD));
        } else if (srcAsmType.equals(AsmType.WORD)) {
            // unsigned int -> long -> double
            target.add(new ZeroExtend(AsmType.WORD, AsmType.DWORD, src, T0));
            target.add(new DoubleFromInt(T0, dst, AsmType.DWORD));
        } else if (srcAsmType.equals(AsmType.DWORD)) {
            // unsigned long -> double
            String labelOutOfRange = makeBackendLabel("out_of_range");
            String labelEnd = makeBackendLabel("end_of_ul2d");

            // 检查是否在 long 内
            target.add(new BranchIfComparison(Comparison.LESS, false, AsmType.DWORD, src, ZERO, labelOutOfRange));

            // 在 long 内，直接转换
            target.add(new DoubleFromInt(src, dst, AsmType.DWORD));
            target.add(new Branch(labelEnd));

            // 不在 long 中，右移后再左移
            target.add(new Label(labelOutOfRange));
            target.add(new Move(AsmType.DWORD, src, T0));
            target.add(new Bitwise(BinaryOperator.BITWISE_AND, AsmType.DWORD, T0, new Immediate(1), T1));
            target.add(new BitwiseShift(false, AsmType.DWORD, true, T0, new Immediate(1), T0));
            target.add(new Bitwise(BinaryOperator.BITWISE_OR, AsmType.DWORD, T0, T1, T1));
            target.add(new DoubleFromInt(T1, dst, AsmType.DWORD));
            target.add(new Binary(BinaryOperator.ADD, AsmType.DOUBLE, dst, dst, dst));

            target.add(new Label(labelEnd));
        } else {
            throw new IllegalStateException("Unexpected asm type: " + srcAsmType);
        }
        return null;
    }

    @Override
    public Void visit(TacGetAddress inst) {
        HighLevelOperand src = lowerValue(inst.src);
        HighLevelOperand dst = lowerValue(inst.dst);
        target.add(new LoadAddress(src, dst));
        return null;
    }

    @Override
    public Void visit(TacLoad inst) {
        TacAddressDescriptor addr = inst.srcAddr.fold();
        checkCharacterType(addr.base());
        if (addr.index() != null) {
            checkCharacterType(addr.index());
        }

        HighLevelOperand dst = lowerValue(inst.dst);
        AsmType asmType = getType(inst.dst).toAsmType();

        HighLevelOperand base = lowerValue(addr.base());

        if (addr.index() == null) {
            // 没有索引
            if (base instanceof Immediate imm) {
                // 常量地址
                if (BitMath.isSi12(imm.value())) {
                    target.add(new Load(asmType, ZERO, imm, dst));
                } else {
                    target.add(new Move(AsmType.DWORD, imm, T0));
                    target.add(new Load(asmType, T0, new Immediate(0), dst));
                }
            } else {
                target.add(new Load(asmType, base, new Immediate(addr.offset()), dst));
            }
        } else {
            HighLevelOperand index = lowerValue(addr.index());
            // 有索引
            if (addr.scale() == 1 && addr.offset() == 0) {
                target.add(new Load(asmType, base, index, dst));
            } else {
                addPointer(base, index, addr.scale(), T0);
                target.add(new Load(asmType, T0, new Immediate(addr.offset()), dst));
            }
        }
        return null;
    }

    @Override
    public Void visit(TacStore inst) {
        TacAddressDescriptor addr = inst.dstAddr.fold();
        checkCharacterType(addr.base());
        if (addr.index() != null) {
            checkCharacterType(addr.index());
        }

        HighLevelOperand src = lowerValue(inst.src);
        AsmType asmType = getType(inst.src).toAsmType();

        HighLevelOperand base = lowerValue(addr.base());

        if (addr.index() == null) {
            // 没有索引
            if (base instanceof Immediate imm) {
                // 常量地址
                if (BitMath.isSi12(imm.value())) {
                    target.add(new Store(asmType, src, ZERO, imm));
                } else {
                    target.add(new Move(AsmType.DWORD, imm, T0));
                    target.add(new Store(asmType, src, T0, new Immediate(0)));
                }
            } else {
                target.add(new Store(asmType, src, base, new Immediate(addr.offset())));
            }
        } else {
            HighLevelOperand index = lowerValue(addr.index());
            // 有索引
            if (addr.scale() == 1 && addr.offset() == 0) {
                target.add(new Store(asmType, src, base, index));
            } else {
                addPointer(base, index, addr.scale(), T0);
                target.add(new Store(asmType, src, T0, new Immediate(addr.offset())));
            }
        }
        return null;
    }

    @Override
    public Void visit(TacAddPointer inst) {
        checkCharacterType(inst.ptr);
        checkCharacterType(inst.index);
        addPointer(lowerValue(inst.ptr), lowerValue(inst.index), inst.scale, lowerValue(inst.dst));
        return null;
    }

    private void addPointer(HighLevelOperand ptr, HighLevelOperand index, long scale, HighLevelOperand dst) {
        if (index instanceof Immediate immIndex) {
            // index 为常量，直接计算偏移量
            long scaledOffset = immIndex.value() * scale;
            target.add(new Binary(BinaryOperator.ADD, AsmType.DWORD, ptr, new Immediate(scaledOffset), dst));
        } else if (scale == 1) {
            // scale 为 1，普通加法
            target.add(new Binary(BinaryOperator.ADD, AsmType.DWORD, ptr, index, dst));
        } else if (scale == 2 || scale == 4 || scale == 8 || scale == 16) {
            // 特殊 scale，使用 alsl
            int shiftAmount = switch ((int) scale) {
                case 2 -> 1;
                case 4 -> 2;
                case 8 -> 3;
                case 16 -> 4;
                default -> throw new IllegalArgumentException("Unsupported scale: " + scale);
            };
            target.add(new AddLeftShift(ptr, index, shiftAmount, dst));
        } else {
            // 对于任意的 scale，使用乘法
            target.add(new Binary(BinaryOperator.MULTIPLY, AsmType.DWORD, index, new Immediate(scale), T1));
            target.add(new Binary(BinaryOperator.ADD, AsmType.DWORD, ptr, T1, dst));
        }
    }

    @Override
    public Void visit(TacCopyToOffset inst) {
        AsmType asmType = getType(inst.src).toAsmType();
        target.add(new Move(asmType, lowerValue(inst.src), new PseudoMemory(inst.dst, inst.offset)));
        return null;
    }

    @Override
    public Void visit(TacIndirectCall inst) {
        passArguments(inst.args);
        target.add(new CallIndirect(lowerValue(inst.funcPtr)));
        getReturnValue(inst.dst);
        return null;
    }

    @Override
    public Void visit(TacCopyByteArrayToOffset inst) {
        target.add(new CopyByteArray(inst.data, new PseudoMemory(inst.dst, inst.offset)));
        return null;
    }

    // 浮点常量池
    private final Map<Double, String> doubleConstants = new HashMap<>();

    private HighLevelOperand lowerValue(TacValue tacValue) {
        if (tacValue instanceof TacConstant tacConstant) {
            Constant constant = tacConstant.value;
            if (constant instanceof ConstantDouble constDouble) {
                long rawDigits = constDouble.toByteRepresentation();

                // 如果浮点数可被简易加载 (2条指令内)，则直接使用 Immediate
                if (LA64Assembler.getExpandLiDSize(rawDigits) <= 2) {
                    return new Immediate(rawDigits);
                }
                // 否则使用浮点常量池
                String label = doubleConstants.computeIfAbsent(
                    constDouble.value(), k -> "const_double_" + doubleConstants.size());
                return new Data(label);
            }
            return immediate(constant);
        } else if (tacValue instanceof TacVariable tacVariable) {
            if (getType(tacVariable).isAggregate()) {
                return new PseudoMemory(tacVariable.name, 0);
            } else {
                return new Pseudo(tacVariable.name);
            }
        }
        throw new UnsupportedOperationException("Unsupported value type: " + tacValue.getClass().getSimpleName());
    }

    private Type getType(TacValue tacValue) {
        if (tacValue instanceof TacConstant tacConstant) {
            return tacConstant.value.getType();
        } else if (tacValue instanceof TacVariable tacVariable) {
            return symbolTable.get(tacVariable.name).type;
        }
        throw new UnsupportedOperationException("Unsupported value type: " + tacValue.getClass().getSimpleName());
    }
}
