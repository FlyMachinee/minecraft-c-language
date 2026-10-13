package net.flymachine.minecraftclanguage.content.logic.compiler.backend.la64;

import net.flymachine.minecraftclanguage.content.logic.architecture.la64.abi.lp64d.callConv.CallingConvention;
import net.flymachine.minecraftclanguage.content.logic.architecture.la64.abi.lp64d.callConv.Placement;
import net.flymachine.minecraftclanguage.content.logic.architecture.la64.abi.lp64d.callConv.Signature;
import net.flymachine.minecraftclanguage.content.logic.architecture.la64.abi.lp64d.callConv.Slot;
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
import net.flymachine.minecraftclanguage.content.logic.compiler.common.type.FunctionType;
import net.flymachine.minecraftclanguage.content.logic.compiler.common.type.PointerType;
import net.flymachine.minecraftclanguage.content.logic.compiler.common.type.Type;
import net.flymachine.minecraftclanguage.content.logic.compiler.frontend.c99.SymbolTable;
import net.flymachine.minecraftclanguage.content.logic.compiler.ir.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

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
                boolean onStack = false;
                FunctionType funcType = (FunctionType) entry.type;
                if (funcType.returnType().isComplete()) {
                    Placement retPlacement = CallingConvention.allocateReturn(funcType.returnType());
                    onStack = retPlacement instanceof Placement.ByReference;
                }
                backendSymbolTable.put(name, new BackendSymbolTable.FuncEntry(entry.attr.isDefinition(), onStack));
            } else if (entry.attr instanceof SymbolTable.Entry.ConstantAttr) {
                AsmType asmType = entry.type.toAsmType();
                backendSymbolTable.put(name, new BackendSymbolTable.ObjectEntry(asmType, true, true));
            } else if (entry.attr instanceof SymbolTable.Entry.AutoAttr autoAttr && autoAttr.passByReference) {
                // 大结构体/联合体按引用传递
                assert entry.type.isRecord() && entry.type.sizeof() > 16;
                backendSymbolTable.put(name, new BackendSymbolTable.ObjectEntry(AsmType.DWORD, false, false));
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

    private long maxCallStackArgSize;

    private Immediate immediate(Constant constant) {
        return new Immediate(constant.toByteRepresentation());
    }

    private int labelCounter = 0;

    private String makeBackendLabel(String prefix) {
        return prefix + "_backend" + labelCounter++;
    }

    private Signature functionSignature = null;

    private HighLevelFunction lowerFunction(TacFunction tacFunction) {
        target = new ArrayList<>();
        maxCallStackArgSize = 0;

        SymbolTable.Entry entry = symbolTable.get(tacFunction.name);
        FunctionType funcType = (FunctionType) entry.type;
        Signature signature = CallingConvention.allocate(funcType);

        if (signature.ret() instanceof Placement.ByReference) {
            // 大结构体/联合体返回，需要在调用者分配空间，并传递地址给被调用函数
            // 对地址进行保存
            assert funcType.returnType().isRecord() && funcType.returnType().sizeof() > 16;
            target.add(new Move(AsmType.DWORD, A0, new Memory(FP, -24)));
        }

        // 将操作数拷贝至伪操作数
        for (int i = 0; i < tacFunction.params.size(); i++) {
            Placement placement = signature.params().get(i);
            HighLevelOperand dst = lowerValue(new TacVariable(tacFunction.params.get(i)));
            Type paramType = funcType.parameterType(i);

            if (placement instanceof Placement.Ignored) {
                throw new IllegalStateException("Control should not reach here");
            }

            if (placement instanceof Placement.ByReference byRef) {
                // 结构体/联合体按引用传递
                assert paramType.isRecord() && paramType.sizeof() > 16;
                // 将传入的地址拷贝至伪操作数
                target.add(new Move(AsmType.DWORD, lowerSlot(byRef.pointerSlot(), false), dst));
                continue;
            }

            Placement.InSlots inSlots = (Placement.InSlots) placement;

            if (paramType.isScalar()) {
                assert inSlots.slots().size() == 1;
                Slot slot = inSlots.slots().get(0);
                target.add(new Move(slot.type(), lowerSlot(slot, false), dst));
            } else if (paramType.isRecord()) {
                // 结构体/联合体类型，按 ABI 要求进行传递
                PseudoMemory dstMem = (PseudoMemory) dst;

                for (Slot slot : inSlots.slots()) {
                    if (slot.type() instanceof AsmType.PrimitiveType) {
                        // 正常拷贝即可
                        target.add(new Move(slot.type(), lowerSlot(slot, false), dstMem));
                    } else {
                        // 非 1 2 4 8 长度，特殊处理
                        AsmType.ByteArray ba = (AsmType.ByteArray) slot.type();
                        if (slot instanceof Slot.GAR gar) {
                            // 通过寄存器传递的小结构体/联合体
                            copyBytesFromReg((int) ba.size(), GPR_ARGS[gar.index()], dstMem);
                        } else if (slot instanceof Slot.Stack stack) {
                            // 通过栈传递的小结构体/联合体
                            copyBytesMemToMem(ba,
                                              new AddrSpec.Dynamic(FP, stack.offset()),
                                              new AddrSpec.Static(dstMem.name(), dstMem.offset()));
                        } else {
                            throw new IllegalStateException("Unknown slot type: " + slot);
                        }
                    }
                    dstMem = new PseudoMemory(dstMem.name(), dstMem.offset() + 8);
                }
            }
        }

        functionSignature = signature;
        List<TacInstruction> instructions = tacFunction.insts;
        for (TacInstruction instruction : instructions) {
            instruction.accept(this);
        }
        functionSignature = null;
        HighLevelFunction function = new HighLevelFunction(tacFunction.name, tacFunction.global, target);
        function.maxCallStackArgSize = maxCallStackArgSize;
        return function;
    }

    @Override
    public Void visit(TacReturn inst) {
        if (inst.value == null) {
            target.add(new Ret());
            return null;
        }

        Placement retPlacement = functionSignature.ret();
        Type retType = getType(inst.value);
        HighLevelOperand val = lowerValue(inst.value);

        if (retPlacement instanceof Placement.Ignored) {
            throw new IllegalStateException("Control should not reach here");
        }
        if (retPlacement instanceof Placement.ByReference) {
            // 返回大结构体/联合体，将结构体/联合体写回先前调用者传入的地址
            assert retType.isRecord() && retType.sizeof() > 16;
            // 先前调用者传入的地址在 fp-24 处，按 ABI 要求通过 A0 传回
            target.add(new Move(AsmType.DWORD, new Memory(FP, -24), A0));
            // 进行写回
            PseudoMemory valMem = (PseudoMemory) val;
            copyBytesMemToMem((AsmType.ByteArray) retType.toAsmType(),
                              new AddrSpec.Static(valMem.name(), valMem.offset()),
                              new AddrSpec.Dynamic(A0, 0));
        } else {
            // 通过寄存器返回
            Placement.InSlots inSlots = (Placement.InSlots) retPlacement;
            if (retType.isScalar()) {
                assert inSlots.slots().size() == 1;
                Slot slot = inSlots.slots().get(0);
                assert slot instanceof Slot.GAR || slot instanceof Slot.FAR;
                // 标量返回，拷贝即可
                target.add(new Move(slot.type(), val, lowerSlot(slot, false)));
            } else if (retType.isRecord()) {
                // 结构体/联合体类型，按 ABI 要求进行传递
                PseudoMemory valMem = (PseudoMemory) val;
                assert retType.sizeof() <= 16;
                // 0 < size <= 16
                for (Slot slot : inSlots.slots()) {
                    if (slot.type() instanceof AsmType.PrimitiveType) {
                        assert slot instanceof Slot.GAR || slot instanceof Slot.FAR;
                        // 正常拷贝即可
                        target.add(new Move(slot.type(), valMem, lowerSlot(slot, false)));
                    } else {
                        assert slot instanceof Slot.GAR;
                        // 非 1 2 4 8 长度，特殊处理
                        AsmType.ByteArray ba = (AsmType.ByteArray) slot.type();
                        Slot.GAR gar = (Slot.GAR) slot;
                        copyBytesToReg((int) ba.size(), valMem, GPR_ARGS[gar.index()]);
                    }
                    valMem = new PseudoMemory(valMem.name(), valMem.offset() + 8);
                }
            }
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

        if (srcType instanceof AsmType.ByteArray byteArray) {
            // 结构体/联合体拷贝
            assert byteArray.equals(dstType);
            PseudoMemory srcMem = (PseudoMemory) src;
            PseudoMemory dstMem = (PseudoMemory) dst;
            copyBytesMemToMem(byteArray,
                              new AddrSpec.Static(srcMem.name(), srcMem.offset()),
                              new AddrSpec.Static(dstMem.name(), dstMem.offset()));
            return null;
        }

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

    private HighLevelOperand lowerSlot(Slot slot, boolean isCaller) {
        if (slot instanceof Slot.GAR gar) {
            return GPR_ARGS[gar.index()];
        } else if (slot instanceof Slot.FAR far) {
            return FPR_ARGS[far.index()];
        } else if (slot instanceof Slot.Stack stack) {
            return new Memory(isCaller ? SP : FP, stack.offset());
        } else {
            throw new IllegalStateException("Unknown slot type: " + slot);
        }
    }

    private void passArguments(List<TacValue> args, @Nullable TacValue dst, Signature signature) {
        // 参数传递逻辑
        // 不需要分配或回收栈空间，由函数序言和尾声进行处理，函数序言中会分配好足够使用的栈空间

        // 处理返回值，大结构体/联合体返回需要通过 a0 传递返回值地址
        if (signature.ret() instanceof Placement.ByReference) {
            assert dst != null;
            target.add(new LoadAddress(lowerValue(dst), A0));
        }

        // 参数传递
        int argSize = args.size();
        for (int i = 0; i < argSize; i++) {
            Placement placement = signature.params().get(i);

            if (placement instanceof Placement.Ignored) {
                throw new IllegalStateException("Control should not reach here");
            }

            TacValue arg = args.get(i);
            Type argType = getType(arg);
            HighLevelOperand argOp = lowerValue(arg);

            if (placement instanceof Placement.ByReference byRef) {
                // 结构体/联合体按引用传递
                assert argType.isRecord();
                PseudoMemory argMem = (PseudoMemory) argOp;
                // 调用者分配空间，并传递地址
                long copyDstOffset = byRef.copySlot().offset();
                // 地址传递
                target.add(new LoadAddress(new Memory(SP, copyDstOffset), lowerSlot(byRef.pointerSlot(), true)));
                // 拷贝结构体/联合体至分配的空间
                AsmType.ByteArray asmType = (AsmType.ByteArray) argType.toAsmType();
                copyBytesMemToMem(asmType,
                                  new AddrSpec.Static(argMem.name(), argMem.offset()),
                                  new AddrSpec.Dynamic(SP, copyDstOffset));
                continue;
            }

            var inSlots = (Placement.InSlots) placement;

            if (argType.isScalar()) {
                assert inSlots.slots().size() == 1;
                Slot slot = inSlots.slots().get(0);
                // 标量类型，按 ABI 要求进行拓展后传递
                AsmType.PrimitiveType asmType = (AsmType.PrimitiveType) argType.toAsmType();
                HighLevelOperand slotOp = lowerSlot(slot, true);
                switch (asmType.p()) {
                    case BYTE, HALF, WORD -> // 进行符号拓展
                        target.add(new SignExtend(asmType, AsmType.DWORD, argOp, slotOp));
                    case UBYTE, UHALF, UWORD -> // 进行零拓展
                        target.add(new ZeroExtend(asmType, AsmType.DWORD, argOp, slotOp));
                    case DWORD, DOUBLE -> target.add(new Move(asmType, argOp, slotOp));
                }
            } else if (argType.isRecord()) {
                // 结构体/联合体类型，按 ABI 要求进行传递
                PseudoMemory argMem = (PseudoMemory) argOp;

                // 在此处，0 < size <= 16，通过寄存器/栈来传递
                for (Slot slot : inSlots.slots()) {
                    if (slot.type() instanceof AsmType.PrimitiveType) {
                        // 正常拷贝即可
                        target.add(new Move(slot.type(), argMem, lowerSlot(slot, true)));
                    } else {
                        // 非 1 2 4 8 长度，特殊处理
                        AsmType.ByteArray ba = (AsmType.ByteArray) slot.type();
                        if (slot instanceof Slot.GAR gar) {
                            // 通过寄存器传递的小结构体/联合体
                            copyBytesToReg((int) ba.size(), argMem, GPR_ARGS[gar.index()]);
                        } else if (slot instanceof Slot.Stack stack) {
                            // 通过栈传递的小结构体/联合体
                            copyBytesMemToMem(ba,
                                              new AddrSpec.Static(argMem.name(), argMem.offset()),
                                              new AddrSpec.Dynamic(SP, stack.offset()));
                        } else {
                            throw new IllegalStateException("Unknown slot type: " + slot);
                        }
                    }
                    argMem = new PseudoMemory(argMem.name(), argMem.offset() + 8);
                }
            } else {
                throw new IllegalStateException("Unsupported argument type: " + argType);
            }
        }

        maxCallStackArgSize = Math.max(maxCallStackArgSize, signature.stackSize());
    }

    private void getReturnValue(@NotNull TacValue dst, Signature signature) {
        HighLevelOperand dstOp = lowerValue(dst);
        Type dstType = getType(dst);
        Placement retPlacement = signature.ret();
        if (retPlacement instanceof Placement.Ignored) {
            throw new IllegalStateException("Control should not reach here");
        } else if (retPlacement instanceof Placement.ByReference) {
            // 结构体/联合体按引用返回，返回值已经通过 a0 传递了
            return;
        }

        // 通过寄存器返回
        Placement.InSlots inSlots = (Placement.InSlots) retPlacement;
        if (dstType.isScalar()) {
            assert inSlots.slots().size() == 1;
            Slot slot = inSlots.slots().get(0);
            HighLevelOperand slotOp = lowerSlot(slot, true);
            AsmType.PrimitiveType asmType = (AsmType.PrimitiveType) dstType.toAsmType();
            target.add(new Move(asmType, slotOp, dstOp));
        } else if (dstType.isRecord()) {
            // 结构体/联合体类型，按 ABI 要求进行返回
            PseudoMemory dstMem = (PseudoMemory) dstOp;

            for (Slot slot : inSlots.slots()) {
                if (slot.type() instanceof AsmType.PrimitiveType) {
                    // 正常拷贝即可
                    target.add(new Move(slot.type(), lowerSlot(slot, true), dstMem));
                } else {
                    // 非 1 2 4 8 长度，特殊处理
                    AsmType.ByteArray ba = (AsmType.ByteArray) slot.type();
                    Slot.GAR gar = (Slot.GAR) slot;
                    copyBytesFromReg((int) ba.size(), GPR_ARGS[gar.index()], dstMem);
                }
                dstMem = new PseudoMemory(dstMem.name(), dstMem.offset() + 8);
            }
        } else {
            throw new IllegalStateException("Unsupported return type: " + dstType);
        }
    }

    @Override
    public Void visit(TacDirectCall inst) {
        SymbolTable.Entry entry = symbolTable.get(inst.funcDesignator);
        Signature signature = CallingConvention.allocate((FunctionType) entry.type);
        passArguments(inst.args, inst.dst, signature);
        target.add(new Call(inst.funcDesignator));
        if (inst.dst != null) {
            getReturnValue(inst.dst, signature);
        }
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

        if (asmType instanceof AsmType.ByteArray byteArray) {
            // 结构体/联合体加载
            PseudoMemory dstMem = (PseudoMemory) dst;
            GeneralPurposeRegister ptr = resolvePtrToT3(addr);
            copyBytesMemToMem(byteArray,
                              new AddrSpec.Dynamic(ptr, addr.offset()),
                              new AddrSpec.Static(dstMem.name(), dstMem.offset()));
            return null;
        }

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

        if (asmType instanceof AsmType.ByteArray byteArray) {
            // 结构体/联合体存储
            PseudoMemory srcMem = (PseudoMemory) src;
            GeneralPurposeRegister ptr = resolvePtrToT3(addr);
            copyBytesMemToMem(byteArray,
                              new AddrSpec.Static(srcMem.name(), srcMem.offset()),
                              new AddrSpec.Dynamic(ptr, addr.offset()));
            return null;
        }

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

        if (asmType instanceof AsmType.ByteArray byteArray) {
            // 结构体/联合体拷贝
            PseudoMemory srcMem = (PseudoMemory) lowerValue(inst.src);
            copyBytesMemToMem(byteArray,
                              new AddrSpec.Static(srcMem.name(), srcMem.offset()),
                              new AddrSpec.Static(inst.dst, inst.offset));
            return null;
        }

        target.add(new Move(asmType, lowerValue(inst.src), new PseudoMemory(inst.dst, inst.offset)));
        return null;
    }

    @Override
    public Void visit(TacIndirectCall inst) {
        Type funcType = getType(inst.funcPtr);
        FunctionType ft = (FunctionType) ((PointerType) funcType).referencedType();
        Signature signature = CallingConvention.allocate(ft);
        passArguments(inst.args, inst.dst, signature);
        target.add(new CallIndirect(lowerValue(inst.funcPtr)));
        if (inst.dst != null) {
            getReturnValue(inst.dst, signature);
        }
        return null;
    }

    @Override
    public Void visit(TacCopyByteArrayToOffset inst) {
        // 按照 8 -> 4 -> 2 -> 1 的顺序拷贝，要求支持非对齐访问
        for (int i = 0; i < inst.data.length; ) {
            if (i + 8 <= inst.data.length) {
                long value = 0;
                for (int j = 0; j < 8; j++) {
                    value |= ((long) (inst.data[i + j] & 0xFF)) << (j * 8);
                }
                target.add(new Move(AsmType.DWORD, new Immediate(value), new PseudoMemory(inst.dst, inst.offset + i)));
                i += 8;
            } else if (i + 4 <= inst.data.length) {
                int value = 0;
                for (int j = 0; j < 4; j++) {
                    value |= ((inst.data[i + j] & 0xFF) << (j * 8));
                }
                target.add(new Move(AsmType.WORD, new Immediate(value), new PseudoMemory(inst.dst, inst.offset + i)));
                i += 4;
            } else if (i + 2 <= inst.data.length) {
                short value = (short) ((inst.data[i] & 0xFF) | ((inst.data[i + 1] & 0xFF) << 8));
                target.add(new Move(AsmType.HALF, new Immediate(value), new PseudoMemory(inst.dst, inst.offset + i)));
                i += 2;
            } else {
                byte value = inst.data[i];
                target.add(new Move(AsmType.BYTE, new Immediate(value), new PseudoMemory(inst.dst, inst.offset + i)));
                ++i;
            }
        }
        return null;
    }

    @Override
    public Void visit(TacCopyFromOffset inst) {
        AsmType asmType = getType(inst.dst).toAsmType();

        if (asmType instanceof AsmType.ByteArray byteArray) {
            // 结构体/联合体拷贝
            PseudoMemory dstMem = (PseudoMemory) lowerValue(inst.dst);
            copyBytesMemToMem(byteArray,
                              new AddrSpec.Static(inst.src, inst.offset),
                              new AddrSpec.Static(dstMem.name(), dstMem.offset()));
            return null;
        }

        target.add(new Move(asmType, new PseudoMemory(inst.src, inst.offset), lowerValue(inst.dst)));
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
            String name = tacVariable.name;
            SymbolTable.Entry entry = symbolTable.get(name);
            if (entry.type.isAggregate() || entry.type.isUnion()) {
                if (entry.attr instanceof SymbolTable.Entry.AutoAttr autoAttr && autoAttr.passByReference) {
                    // 通过引用传递的大结构体/联合体
                    assert entry.type.isRecord() && entry.type.sizeof() > 16;
                    return new Pseudo(tacVariable.name);
                } else {
                    return new PseudoMemory(tacVariable.name, 0);
                }
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
            SymbolTable.Entry entry = symbolTable.get(tacVariable.name);
            if (entry.attr instanceof SymbolTable.Entry.AutoAttr autoAttr && autoAttr.passByReference) {
                // 通过引用传递的大结构体/联合体
                assert entry.type.isRecord() && entry.type.sizeof() > 16;
                return new PointerType(entry.type);
            }
            return entry.type;
        }
        throw new UnsupportedOperationException("Unsupported value type: " + tacValue.getClass().getSimpleName());
    }

    private sealed interface AddrSpec {
        record Static(String name, long baseOffset) implements AddrSpec { }

        record Dynamic(GeneralPurposeRegister ptr, long baseOffset) implements AddrSpec { }
    }

    private void copyBytesMemToMem(AsmType.ByteArray byteArray, AddrSpec src, AddrSpec dst) {
        for (long offset = 0; offset < byteArray.size(); ) {
            if (offset + 8 <= byteArray.size()) {
                emitRead(AsmType.DWORD, src, offset, T1);
                emitWrite(AsmType.DWORD, T1, dst, offset);
                offset += 8;
            } else if (offset + 4 <= byteArray.size()) {
                emitRead(AsmType.WORD, src, offset, T1);
                emitWrite(AsmType.WORD, T1, dst, offset);
                offset += 4;
            } else if (offset + 2 <= byteArray.size()) {
                emitRead(AsmType.HALF, src, offset, T1);
                emitWrite(AsmType.HALF, T1, dst, offset);
                offset += 2;
            } else {
                emitRead(AsmType.BYTE, src, offset, T1);
                emitWrite(AsmType.BYTE, T1, dst, offset);
                ++offset;
            }
        }
    }

    private void emitRead(AsmType t, AddrSpec addr, long offset, GeneralPurposeRegister dst) {
        if (addr instanceof AddrSpec.Static s) {
            target.add(new Move(t, new PseudoMemory(s.name(), s.baseOffset() + offset), dst));
        } else if (addr instanceof AddrSpec.Dynamic d) {
            target.add(new Load(t, d.ptr(), new Immediate(d.baseOffset() + offset), dst));
        } else {
            throw new IllegalStateException("Unexpected AddrSpec type: " + addr.getClass().getSimpleName());
        }
    }

    private void emitWrite(AsmType t, GeneralPurposeRegister src, AddrSpec addr, long offset) {
        if (addr instanceof AddrSpec.Static s) {
            target.add(new Move(t, src, new PseudoMemory(s.name(), s.baseOffset() + offset)));
        } else if (addr instanceof AddrSpec.Dynamic d) {
            target.add(new Store(t, src, d.ptr(), new Immediate(d.baseOffset() + offset)));
        } else {
            throw new IllegalStateException("Unexpected AddrSpec type: " + addr.getClass().getSimpleName());
        }
    }

    private GeneralPurposeRegister resolvePtrToT3(TacAddressDescriptor addr) {
        HighLevelOperand base = lowerValue(addr.base());
        if (addr.index() == null) {
            target.add(new Move(AsmType.DWORD, base, T3));
        } else {
            addPointer(base, lowerValue(addr.index()), addr.scale(), T3);
        }
        return T3;
    }

    private void copyBytesToReg(int length, PseudoMemory src, GeneralPurposeRegister dst) {
        // 将指定长度的字节从内存拷贝到寄存器中
        // 实现为 4 -> 2 -> 1 的加载 + 移位 + 合并序列
        // 注意是小端序
        assert length == 3 || length == 5 || length == 6 || length == 7;
        int shift = 0;
        boolean first = true;
        for (int offset = 0; offset < length; ) {
            int remaining = length - offset;
            AsmType type;
            int width;
            if (remaining >= 4) {
                type = AsmType.UWORD;
                width = 4;
            } else if (remaining >= 2) {
                type = AsmType.UHALF;
                width = 2;
            } else {
                type = AsmType.UBYTE;
                width = 1;
            }
            PseudoMemory srcOffset = new PseudoMemory(src.name(), src.offset() + offset);
            if (first) {
                // 加载最低部分到 dst
                target.add(new Move(type, srcOffset, dst));
                first = false;
            } else {
                // 后续先加载到 T1，再按已累积的位宽左移，最后按位或到 dst
                target.add(new Move(type, srcOffset, T0));
                assert shift > 0;
                target.add(new BitwiseShift(true, AsmType.DWORD, false, T0, new Immediate(shift), T0));
                target.add(new Bitwise(BinaryOperator.BITWISE_OR, AsmType.DWORD, dst, T0, dst));
            }
            offset += width;
            shift += width * 8;
        }
    }

    private void copyBytesFromReg(int length, GeneralPurposeRegister src, PseudoMemory dst) {
        // 将指定长度的字节从寄存器拷贝到内存中
        // 实现为 4 -> 2 -> 1 的存储 + 逐步右移序列
        // 注意是小端序
        assert length == 3 || length == 5 || length == 6 || length == 7;
        target.add(new Move(AsmType.DWORD, src, T0));
        for (int remaining = length, offset = 0; remaining > 0; ) {
            int width;
            AsmType type;
            if (remaining >= 4) {
                width = 4;
                type = AsmType.WORD;
            } else if (remaining >= 2) {
                width = 2;
                type = AsmType.HALF;
            } else {
                width = 1;
                type = AsmType.BYTE;
            }
            target.add(new Move(type, T0, new PseudoMemory(dst.name(), dst.offset() + offset)));
            offset += width;
            remaining -= width;
            if (remaining > 0) {
                target.add(new BitwiseShift(false, AsmType.DWORD, true, T0, new Immediate(width * 8), T0));
            }
        }
    }
}
