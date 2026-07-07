package net.flymachine.minecraftclanguage.content.logic.compiler.backend.la64;

import net.flymachine.minecraftclanguage.content.logic.architecture.la64.register.GeneralPurposeRegister;
import net.flymachine.minecraftclanguage.content.logic.compiler.backend.la64.highLevel.*;
import net.flymachine.minecraftclanguage.content.logic.compiler.backend.la64.highLevel.instruction.*;
import net.flymachine.minecraftclanguage.content.logic.compiler.backend.la64.highLevel.operand.HighLevelOperand;
import net.flymachine.minecraftclanguage.content.logic.compiler.backend.la64.highLevel.operand.Immediate;
import net.flymachine.minecraftclanguage.content.logic.compiler.backend.la64.highLevel.operand.Pseudo;
import net.flymachine.minecraftclanguage.content.logic.compiler.backend.la64.highLevel.operand.Stack;
import net.flymachine.minecraftclanguage.content.logic.compiler.common.BinaryOperator;
import net.flymachine.minecraftclanguage.content.logic.compiler.common.Comparison;
import net.flymachine.minecraftclanguage.content.logic.compiler.common.constant.Constant;
import net.flymachine.minecraftclanguage.content.logic.compiler.common.type.BasicType;
import net.flymachine.minecraftclanguage.content.logic.compiler.common.type.FunctionType;
import net.flymachine.minecraftclanguage.content.logic.compiler.common.type.Type;
import net.flymachine.minecraftclanguage.content.logic.compiler.frontend.c99.SymbolTable;
import net.flymachine.minecraftclanguage.content.logic.compiler.ir.*;

import java.util.ArrayList;
import java.util.List;

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
                    new BackendSymbolTable.ObjectEntry(asmType, entry.attr instanceof SymbolTable.Entry.StaticAttr));
            }
        }
        return new HighLevelProgram(topLevels);
    }

    private List<HighLevelInstruction> target;

    private static final GeneralPurposeRegister[] ARG_REGS = {A0, A1, A2, A3, A4, A5, A6, A7};

    private int maxCallStackArgSize;

    private Immediate immediate(Constant constant) {
        return new Immediate(constant.toLong().value());
    }

    private HighLevelFunction lowerFunction(TacFunction tacFunction) {
        target = new ArrayList<>();
        maxCallStackArgSize = 0;

        // 拷贝参数至栈上
        int argIndex = 0;
        while (argIndex < Math.min(ARG_REGS.length, tacFunction.params.size())) {
            String name = tacFunction.params.get(argIndex);
            AsmType asmType = symbolTable.get(name).type.toAsmType();
            target.add(new Move(asmType, ARG_REGS[argIndex], new Pseudo(name)));
            argIndex++;
        }

        int stackOffset = 0;
        while (argIndex < tacFunction.params.size()) {
            String name = tacFunction.params.get(argIndex);
            AsmType asmType = symbolTable.get(name).type.toAsmType();
            target.add(new Move(asmType, new Stack(stackOffset, true), new Pseudo(name)));
            stackOffset += 8;
            argIndex++;
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
        target.add(new Move(asmType, src, A0));
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
            AsmType asmType = getType(inst.lhs).toAsmType();
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
                    boolean isUnsigned = ((BasicType) getType(inst.lhs)).isUnsigned();
                    target.add(new DivOrMod(
                        inst.op == BinaryOperator.DIVIDE, asmType, isUnsigned,
                        lowerValue(inst.lhs), lowerValue(inst.rhs), lowerValue(inst.dst)));
                }
                case LEFT_SHIFT, RIGHT_SHIFT -> {
                    boolean isLeftShift = inst.op == BinaryOperator.LEFT_SHIFT;
                    boolean isUnsigned = ((BasicType) getType(inst.lhs)).isUnsigned();
                    target.add(new BitwiseShift(
                        isLeftShift, asmType, isUnsigned,
                        lowerValue(inst.lhs), lowerValue(inst.rhs), lowerValue(inst.dst)));
                }
                case LOGICAL_AND, LOGICAL_OR ->
                    throw new UnsupportedOperationException("Logical operators should be lowered to branches");
                case LESS_THAN, GREATER_THAN, LESS_OR_EQUAL, GREATER_OR_EQUAL, EQUAL, NOT_EQUAL -> {
                    Comparison cond = inst.op.toComparison();
                    boolean isUnsigned = ((BasicType) getType(inst.lhs)).isUnsigned();
                    target.add(new Compare(
                        cond, isUnsigned, asmType,
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
        } else {
            AsmType asmType = getType(inst.cond).toAsmType();
            target.add(new BranchIfZero(asmType, lowerValue(inst.cond), inst.target));
        }
        return null;
    }

    @Override
    public Void visit(TacJumpIfNotZero inst) {
        if (inst.cond instanceof TacConstant tacConstant) {
            // 常量检查
            if (!tacConstant.value.isZero()) {
                target.add(new Branch(inst.target));
            }
        } else {
            AsmType asmType = getType(inst.cond).toAsmType();
            target.add(new BranchIfNotZero(asmType, lowerValue(inst.cond), inst.target));
        }
        return null;
    }

    @Override
    public Void visit(TacJumpIfComparison inst) {
        if (inst.lhs instanceof TacConstant lhsConstant && inst.rhs instanceof TacConstant rhsConstant) {
            // 常量检查
            boolean conditionMet = !lhsConstant.value.apply(inst.cond, rhsConstant.value).isZero();
            if (conditionMet) {
                target.add(new Branch(inst.target));
            }
        } else {
            boolean isUnsigned = ((BasicType) getType(inst.lhs)).isUnsigned();
            AsmType asmType = getType(inst.lhs).toAsmType();
            target.add(new BranchIfComparison(
                inst.cond, isUnsigned, asmType,
                lowerValue(inst.lhs), lowerValue(inst.rhs), inst.target));
        }
        return null;
    }

    @Override
    public Void visit(TacFunctionCall inst) {
        // 参数传递
        // 前八个使用 a0 -> a1 -> ... -> a7
        // 接下来使用 0(sp) -> 8(sp) -> ...
        // 不需要分配或回收栈空间，由函数序言和尾声进行处理，函数序言中会分配好足够使用的栈空间

        // 寄存器传递参数
        int argIndex = 0;
        while (argIndex < Math.min(ARG_REGS.length, inst.args.size())) {
            HighLevelOperand arg = lowerValue(inst.args.get(argIndex));
            AsmType asmType = getType(inst.args.get(argIndex)).toAsmType();
            target.add(new Move(asmType, arg, ARG_REGS[argIndex]));
            argIndex++;
        }

        // 栈传递参数
        int stackOffset = 0;
        while (argIndex < inst.args.size()) {
            HighLevelOperand arg = lowerValue(inst.args.get(argIndex));
            AsmType asmType = getType(inst.args.get(argIndex)).toAsmType();
            target.add(new Move(asmType, arg, new Stack(stackOffset, false)));
            stackOffset += 8;
            argIndex++;
        }

        maxCallStackArgSize = Math.max(maxCallStackArgSize, stackOffset);

        // 调用函数
        target.add(new Call(inst.funcName));

        // 获取返回值
        HighLevelOperand dst = lowerValue(inst.dst);
        AsmType asmType = getType(inst.dst).toAsmType();
        target.add(new Move(asmType, A0, dst));
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

    private HighLevelOperand lowerValue(TacValue tacValue) {
        if (tacValue instanceof TacConstant tacConstant) {
            return immediate(tacConstant.value);
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
