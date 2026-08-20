package net.flymachine.minecraftclanguage.content.logic.compiler.backend.la64;

import net.flymachine.minecraftclanguage.content.logic.architecture.la64.isa.instruction.LA64FloatCompareCondition;
import net.flymachine.minecraftclanguage.content.logic.architecture.la64.register.FloatingPointRegister;
import net.flymachine.minecraftclanguage.content.logic.architecture.la64.register.GeneralPurposeRegister;
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
import net.flymachine.minecraftclanguage.content.logic.compiler.common.type.FunctionType;
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
                    new HighLevelStaticVar(staticVar.name, staticVar.global, staticVar.type.sizeof(), staticVar.init));
            }
        }
        // 建立后端符号表
        for (SymbolTable.Entry entry : symbolTable.getEntries()) {
            if (entry.type instanceof FunctionType) {
                backendSymbolTable.put(entry.id.name, new BackendSymbolTable.FuncEntry(entry.attr.isDefinition()));
            } else {
                AsmType asmType = entry.type.toAsmType();
                backendSymbolTable.put(
                    entry.id.name,
                    new BackendSymbolTable.ObjectEntry(
                        asmType, entry.attr instanceof SymbolTable.Entry.StaticAttr, false));
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
        return new Immediate(constant.toLong().value());
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
            if (asmType == AsmType.DOUBLE) {
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
            target.add(new Move(asmType, new Stack(stackOffset, true), new Pseudo(name)));
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
        if (asmType == AsmType.DOUBLE) {
            target.add(new Move(AsmType.DOUBLE, src, FA0));
        } else {
            target.add(new Move(asmType, src, A0));
        }
        target.add(new Ret());
        return null;
    }

    @Override
    public Void visit(TacUnaryOperation inst) {
        if (inst.src instanceof TacConstant tacConstant) {
            // 若源操作数为常量，直接计算结果并生成 Move 指令
            Constant result = tacConstant.value.apply(inst.op);
            AsmType asmType = getType(inst.dst).toAsmType();
            target.add(new Move(asmType, immediate(result), lowerValue(inst.dst)));
        } else {
            AsmType asmType = getType(inst.src).toAsmType();
            switch (inst.op) {
                case NEGATE -> {
                    if (asmType == AsmType.DOUBLE) {
                        // fneg.d
                        target.add(new DoubleNegate(lowerValue(inst.src), lowerValue(inst.dst)));
                        return null;
                    }

                    // sub.w(d) dst r0 src
                    target.add(new Binary(
                        Binary.Operator.SUB, asmType,
                        ZERO, lowerValue(inst.src), lowerValue(inst.dst)));
                }
                case COMPLEMENT -> {
                    // nor dst src r0
                    target.add(new Bitwise(
                        Bitwise.Operator.NOR, asmType,
                        lowerValue(inst.src), ZERO, lowerValue(inst.dst)));
                }
                case NOT -> {
                    if (asmType == AsmType.DOUBLE) {
                        target.add(new Move(AsmType.DOUBLE, ZERO, FT1));
                        target.add(new CompareDouble(CEQ, lowerValue(inst.src), FT1, FCC0));
                        target.add(new GetCC(FCC0, lowerValue(inst.dst)));
                        return null;
                    }

                    // sltui dst src 1
                    target.add(new Compare(
                        Comparison.LESS, true, asmType,
                        lowerValue(inst.src), new Immediate(1), lowerValue(inst.dst)));
                }
            }
        }
        return null;
    }

    @Override
    public Void visit(TacBinaryOperation inst) {
        if (inst.lhs instanceof TacConstant tacLhsConstant &&
            inst.rhs instanceof TacConstant tacRhsConstant) {
            // 若左、右操作数均为常量，直接计算结果并生成 Move 指令
            Constant result = tacLhsConstant.value.apply(inst.op, tacRhsConstant.value);
            AsmType asmType = getType(inst.dst).toAsmType();
            target.add(new Move(asmType, immediate(result), lowerValue(inst.dst)));
        } else {
            Type lhsType = getType(inst.lhs);
            AsmType asmType = lhsType.toAsmType();
            switch (inst.op) {
                case ADD, SUBTRACT, MULTIPLY -> {
                    target.add(new Binary(
                        inst.op, asmType,
                        lowerValue(inst.lhs), lowerValue(inst.rhs), lowerValue(inst.dst)));
                }
                case BITWISE_AND, BITWISE_OR, BITWISE_XOR -> {
                    target.add(new Bitwise(
                        inst.op, asmType,
                        lowerValue(inst.lhs), lowerValue(inst.rhs), lowerValue(inst.dst)));
                }
                case DIVIDE, MODULO -> {
                    boolean isUnsigned = ((BasicType) lhsType).isUnsigned();
                    target.add(new DivOrMod(
                        inst.op == BinaryOperator.DIVIDE, asmType, isUnsigned,
                        lowerValue(inst.lhs), lowerValue(inst.rhs), lowerValue(inst.dst)));
                }
                case LEFT_SHIFT, RIGHT_SHIFT -> {
                    boolean isLeftShift = inst.op == BinaryOperator.LEFT_SHIFT;
                    boolean isUnsigned = ((BasicType) lhsType).isUnsigned();
                    target.add(new BitwiseShift(
                        isLeftShift, asmType, isUnsigned,
                        lowerValue(inst.lhs), lowerValue(inst.rhs), lowerValue(inst.dst)));
                }
                case LOGICAL_AND, LOGICAL_OR ->
                    throw new UnsupportedOperationException("Logical operators should be lowered to branches");
                case LESS_THAN, GREATER_THAN, LESS_OR_EQUAL, GREATER_OR_EQUAL, EQUAL, NOT_EQUAL -> {
                    Comparison cmp = inst.op.toComparison();
                    if (asmType == AsmType.DOUBLE) {
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
                        target.add(new CompareDouble(
                            cond,
                            swap ? lowerValue(inst.rhs) : lowerValue(inst.lhs),
                            swap ? lowerValue(inst.lhs) : lowerValue(inst.rhs), FCC0));
                        target.add(new GetCC(FCC0, lowerValue(inst.dst)));
                        return null;
                    }

                    boolean isUnsigned = ((BasicType) getType(inst.lhs)).isUnsigned();
                    target.add(new Compare(
                        cmp, isUnsigned, asmType,
                        lowerValue(inst.lhs), lowerValue(inst.rhs), lowerValue(inst.dst)));
                }
                default -> throw new UnsupportedOperationException("Unsupported binary operator: " + inst.op);
            }

        }
        return null;
    }

    @Override
    public Void visit(TacCopy inst) {
        AsmType asmType = getType(inst.src).toAsmType();
        target.add(new Move(asmType, lowerValue(inst.src), lowerValue(inst.dst)));
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
        if (asmType == AsmType.DOUBLE) {
            target.add(new Move(AsmType.DOUBLE, ZERO, FT1));
            target.add(new CompareDouble(CEQ, lowerValue(inst.cond), FT1, FCC0));
            target.add(new BranchIfCCNotZero(FCC0, inst.target));
            return null;
        }

        target.add(new BranchIfZero(asmType, lowerValue(inst.cond), inst.target));
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
        if (asmType == AsmType.DOUBLE) {
            target.add(new Move(AsmType.DOUBLE, ZERO, FT1));
            target.add(new CompareDouble(CUNE, lowerValue(inst.cond), FT1, FCC0));
            target.add(new BranchIfCCNotZero(FCC0, inst.target));
            return null;
        }

        target.add(new BranchIfNotZero(asmType, lowerValue(inst.cond), inst.target));
        return null;
    }

    @Override
    public Void visit(TacJumpIfComparison inst) {
        if (inst.lhs instanceof TacConstant lhsConstant && inst.rhs instanceof TacConstant rhsConstant) {
            // 常量检查
            boolean conditionMet = !lhsConstant.value.apply(inst.cond, rhsConstant.value).isZero();
            if (conditionMet ^ inst.inverse) {
                target.add(new Branch(inst.target));
            }
            return null;
        }

        AsmType asmType = getType(inst.lhs).toAsmType();

        if (asmType == AsmType.DOUBLE) {
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
            target.add(new CompareDouble(
                cond,
                swap ? lowerValue(inst.rhs) : lowerValue(inst.lhs),
                swap ? lowerValue(inst.lhs) : lowerValue(inst.rhs), FCC0));
            if (inst.inverse) {
                target.add(new BranchIfCCZero(FCC0, inst.target));
            } else {
                target.add(new BranchIfCCNotZero(FCC0, inst.target));
            }
            return null;
        }

        boolean isUnsigned = ((BasicType) getType(inst.lhs)).isUnsigned();
        Comparison cmp = switch (inst.cond) {
            case EQUAL -> inst.inverse ? Comparison.NOT_EQUAL : Comparison.EQUAL;
            case NOT_EQUAL -> inst.inverse ? Comparison.EQUAL : Comparison.NOT_EQUAL;
            case LESS -> inst.inverse ? Comparison.GREATER_EQUAL : Comparison.LESS;
            case GREATER_EQUAL -> inst.inverse ? Comparison.LESS : Comparison.GREATER_EQUAL;
            case GREATER -> inst.inverse ? Comparison.LESS_EQUAL : Comparison.GREATER;
            case LESS_EQUAL -> inst.inverse ? Comparison.GREATER : Comparison.LESS_EQUAL;
        };
        target.add(new BranchIfComparison(
            cmp, isUnsigned, asmType,
            lowerValue(inst.lhs), lowerValue(inst.rhs), inst.target));
        return null;
    }

    @Override
    public Void visit(TacFunctionCall inst) {
        // 参数传递
        // 不需要分配或回收栈空间，由函数序言和尾声进行处理，函数序言中会分配好足够使用的栈空间

        List<AsmType> asmTypes = inst.args.stream()
                                          .map(arg -> getType(arg).toAsmType())
                                          .toList();
        ArgumentPassingInfo argPassingInfo = getArgumentPassingInfo(asmTypes);

        int gprIndex = 0;
        for (int argIndex : argPassingInfo.gprArgs) {
            HighLevelOperand arg = lowerValue(inst.args.get(argIndex));
            AsmType asmType = asmTypes.get(argIndex);
            target.add(new Move(asmType, arg, GPR_ARGS[gprIndex]));
            gprIndex++;
        }

        int fprIndex = 0;
        for (int argIndex : argPassingInfo.fprArgs) {
            HighLevelOperand arg = lowerValue(inst.args.get(argIndex));
            target.add(new Move(AsmType.DOUBLE, arg, FPR_ARGS[fprIndex]));
            fprIndex++;
        }

        int stackOffset = 0;
        for (int argIndex : argPassingInfo.stackArgs) {
            HighLevelOperand arg = lowerValue(inst.args.get(argIndex));
            AsmType asmType = asmTypes.get(argIndex);
            target.add(new Move(asmType, arg, new Stack(stackOffset, false)));
            stackOffset += 8;
        }

        maxCallStackArgSize = Math.max(maxCallStackArgSize, stackOffset);

        // 调用函数
        target.add(new Call(inst.funcName));

        // 获取返回值
        HighLevelOperand dst = lowerValue(inst.dst);
        AsmType asmType = getType(inst.dst).toAsmType();
        if (asmType == AsmType.DOUBLE) {
            target.add(new Move(AsmType.DOUBLE, FA0, dst));
        } else {
            target.add(new Move(asmType, A0, dst));
        }
        return null;
    }

    @Override
    public Void visit(TacSignExtend inst) {
        HighLevelOperand src = lowerValue(inst.src);
        HighLevelOperand dst = lowerValue(inst.dst);
        target.add(new AddSignExtend(src, dst));
        return null;
    }

    @Override
    public Void visit(TacTruncate inst) {
        HighLevelOperand src = lowerValue(inst.src);
        HighLevelOperand dst = lowerValue(inst.dst);
        target.add(new Move(AsmType.WORD, src, dst));
        return null;
    }

    @Override
    public Void visit(TacZeroExtend inst) {
        HighLevelOperand src = lowerValue(inst.src);
        HighLevelOperand dst = lowerValue(inst.dst);
        target.add(new BstrpickZeroExtend(src, dst));
        return null;
    }

    @Override
    public Void visit(TacDoubleToInt inst) {
        HighLevelOperand src = lowerValue(inst.src);
        HighLevelOperand dst = lowerValue(inst.dst);
        AsmType asmType = getType(inst.dst).toAsmType();
        target.add(new DoubleToIntRoundZero(src, dst, asmType));
        return null;
    }

    @Override
    public Void visit(TacDoubleToUnsignedInt inst) {
        HighLevelOperand src = lowerValue(inst.src);
        HighLevelOperand dst = lowerValue(inst.dst);
        AsmType asmType = getType(inst.dst).toAsmType();

        if (asmType == AsmType.WORD) {
            // double -> unsigned int:
            // double -> long -> unsigned int
            target.add(new DoubleToIntRoundZero(src, T0, AsmType.DWORD));
            target.add(new Move(AsmType.WORD, T0, dst));
        } else if (asmType == AsmType.DWORD) {
            // double -> unsigned long:
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
        target.add(new DoubleFromInt(src, dst, asmType));
        return null;
    }

    @Override
    public Void visit(TacUnsignedIntToDouble inst) {
        HighLevelOperand src = lowerValue(inst.src);
        HighLevelOperand dst = lowerValue(inst.dst);
        AsmType srcAsmType = getType(inst.src).toAsmType();

        if (srcAsmType == AsmType.WORD) {
            // unsigned int -> double:
            // unsigned int -> long -> double
            target.add(new BstrpickZeroExtend(src, T0));
            target.add(new DoubleFromInt(T0, dst, AsmType.DWORD));
        } else if (srcAsmType == AsmType.DWORD) {
            // unsigned long -> double:
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
        return null;
    }

    @Override
    public Void visit(TacLoad inst) {
        return null;
    }

    @Override
    public Void visit(TacStore inst) {
        return null;
    }

    // 浮点常量池
    private final Map<Double, String> doubleConstants = new HashMap<>();

    private HighLevelOperand lowerValue(TacValue tacValue) {
        if (tacValue instanceof TacConstant tacConstant) {
            Constant constant = tacConstant.value;
            if (constant instanceof ConstantDouble constDouble) {
                double doubleValue = constDouble.value();
                long rawDigits = Double.doubleToLongBits(doubleValue);

                // 如果浮点数可被简易加载 (2条指令内)，则直接使用 Immediate
                if (LA64Assembler.getExpandLiDSize(rawDigits) <= 2) {
                    return new Immediate(rawDigits);
                }
                // 否则使用浮点常量池
                String label = doubleConstants.computeIfAbsent(
                    doubleValue, k -> "const_double_" + doubleConstants.size());
                return new Data(label);
            }
            return immediate(constant);
        } else if (tacValue instanceof TacVariable tacVariable) {
            return new Pseudo(tacVariable.name);
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
