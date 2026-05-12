package net.flymachine.minecraftclanguage.content.logic.compiler.backend.la64;

import net.flymachine.minecraftclanguage.content.logic.architecture.la64.register.GeneralPurposeRegister;
import net.flymachine.minecraftclanguage.content.logic.compiler.backend.la64.highLevel.HighLevelFunction;
import net.flymachine.minecraftclanguage.content.logic.compiler.backend.la64.highLevel.HighLevelProgram;
import net.flymachine.minecraftclanguage.content.logic.compiler.backend.la64.highLevel.instruction.*;
import net.flymachine.minecraftclanguage.content.logic.compiler.backend.la64.highLevel.operand.HighLevelOperand;
import net.flymachine.minecraftclanguage.content.logic.compiler.backend.la64.highLevel.operand.Immediate;
import net.flymachine.minecraftclanguage.content.logic.compiler.backend.la64.highLevel.operand.Pseudo;
import net.flymachine.minecraftclanguage.content.logic.compiler.backend.la64.highLevel.operand.Stack;
import net.flymachine.minecraftclanguage.content.logic.compiler.common.BinaryOperator;
import net.flymachine.minecraftclanguage.content.logic.compiler.ir.*;

import java.util.ArrayList;
import java.util.List;

public final class TacToHighLevelAsmLowerer implements TacVisitor<Void> {

    public TacToHighLevelAsmLowerer() { }

    public HighLevelProgram lower(TacProgram tacProgram) {
        List<HighLevelFunction> functions = new ArrayList<>();
        for (TacFunction tacFunction : tacProgram.functionDefinitions) {
            functions.add(lowerFunction(tacFunction));
        }
        return new HighLevelProgram(functions);
    }

    private List<HighLevelInstruction> target;

    private static final GeneralPurposeRegister[] argumentRegisters = {
        GeneralPurposeRegister.A0, GeneralPurposeRegister.A1, GeneralPurposeRegister.A2, GeneralPurposeRegister.A3,
        GeneralPurposeRegister.A4, GeneralPurposeRegister.A5, GeneralPurposeRegister.A6, GeneralPurposeRegister.A7
    };

    private int maxCallStackArgSize;

    private HighLevelFunction lowerFunction(TacFunction tacFunction) {
        target = new ArrayList<>();
        maxCallStackArgSize = 0;

        // 拷贝参数至栈上
        int argIndex = 0;
        while (argIndex < Math.min(argumentRegisters.length, tacFunction.params.size())) {
            target.add(new Move(argumentRegisters[argIndex], new Pseudo(tacFunction.params.get(argIndex))));
            argIndex++;
        }

        int stackOffset = 0;
        while (argIndex < tacFunction.params.size()) {
            target.add(new Move(new Stack(stackOffset, true), new Pseudo(tacFunction.params.get(argIndex))));
            stackOffset += 8;
            argIndex++;
        }

        List<TacInstruction> instructions = tacFunction.instructions;
        for (TacInstruction instruction : instructions) {
            instruction.accept(this);
        }
        HighLevelFunction function = new HighLevelFunction(tacFunction.name, target);
        function.maxCallStackArgSize = maxCallStackArgSize;
        return function;
    }

    @Override
    public Void visit(TacReturn inst) {
        target.add(new Move(lowerValue(inst.value), GeneralPurposeRegister.A0));
        target.add(new Ret());
        return null;
    }

    @Override
    public Void visit(TacUnaryOperation inst) {
        if (inst.src instanceof TacIntConstant tacIntConstant) {
            // 若源操作数为常量，直接计算结果并生成 Move 指令
            int result = switch (inst.op) {
                case NEGATE -> -tacIntConstant.value;
                case COMPLEMENT -> ~tacIntConstant.value;
                case NOT -> tacIntConstant.value == 0 ? 1 : 0;
            };
            target.add(new Move(new Immediate(result), lowerValue(inst.dst)));
        } else {
            target.add(new Unary(
                inst.op,
                lowerValue(inst.src),
                lowerValue(inst.dst)));
        }
        return null;
    }

    @Override
    public Void visit(TacBinaryOperation inst) {
        if (inst.lhs instanceof TacIntConstant tacLhsIntConstant &&
            inst.rhs instanceof TacIntConstant tacRhsIntConstant && !(
            ((inst.op == BinaryOperator.MULTIPLY) ||
             (inst.op == BinaryOperator.DIVIDE)) && tacRhsIntConstant.value == 0
        )) {
            // 若左、右操作数均为常量，直接计算结果并生成 Move 指令
            int result = getResult(inst, tacLhsIntConstant, tacRhsIntConstant);
            target.add(new Move(new Immediate(result), lowerValue(inst.dst)));
        } else {
            target.add(new Binary(
                inst.op,
                lowerValue(inst.lhs),
                lowerValue(inst.rhs),
                lowerValue(inst.dst)));
        }
        return null;
    }

    @Override
    public Void visit(TacCopy inst) {
        target.add(new Move(lowerValue(inst.src), lowerValue(inst.dst)));
        return null;
    }

    @Override
    public Void visit(TacLabel inst) {
        target.add(new Label(inst.identifier));
        return null;
    }

    @Override
    public Void visit(TacJump inst) {
        target.add(new Branch(inst.target));
        return null;
    }

    @Override
    public Void visit(TacJumpIfZero inst) {
        if (inst.cond instanceof TacIntConstant tacIntConstant) {
            // 常量检查
            if (tacIntConstant.value == 0) {
                target.add(new Branch(inst.target));
            }
        } else {
            target.add(new BranchIfZero(lowerValue(inst.cond), inst.target));
        }
        return null;
    }

    @Override
    public Void visit(TacJumpIfNotZero inst) {
        if (inst.cond instanceof TacIntConstant tacIntConstant) {
            // 常量检查
            if (tacIntConstant.value != 0) {
                target.add(new Branch(inst.target));
            }
        } else {
            target.add(new BranchIfNotZero(lowerValue(inst.cond), inst.target));
        }
        return null;
    }

    @Override
    public Void visit(TacJumpIfComparison inst) {
        if (inst.lhs instanceof TacIntConstant lhsIntConstant && inst.rhs instanceof TacIntConstant rhsIntConstant) {
            // 常量检查
            int lhs = lhsIntConstant.value;
            int rhs = rhsIntConstant.value;
            boolean conditionMet = switch (inst.cond) {
                case EQUAL -> lhs == rhs;
                case NOT_EQUAL -> lhs != rhs;
                case LESS -> lhs < rhs;
                case LESS_EQUAL -> lhs <= rhs;
                case GREATER -> lhs > rhs;
                case GREATER_EQUAL -> lhs >= rhs;
            };
            if (conditionMet) {
                target.add(new Branch(inst.target));
            }
        } else {
            target.add(new BranchIfComparison(
                inst.cond,
                lowerValue(inst.lhs),
                lowerValue(inst.rhs),
                inst.target));
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
        while (argIndex < Math.min(argumentRegisters.length, inst.args.size())) {
            target.add(new Move(lowerValue(inst.args.get(argIndex)), argumentRegisters[argIndex]));
            argIndex++;
        }

        // 栈传递参数
        int stackOffset = 0;
        while (argIndex < inst.args.size()) {
            target.add(new Move(lowerValue(inst.args.get(argIndex)), new Stack(stackOffset, false)));
            stackOffset += 8;
            argIndex++;
        }

        maxCallStackArgSize = Math.max(maxCallStackArgSize, stackOffset);

        // 调用函数
        target.add(new Call(inst.funcName));

        // 获取返回值
        target.add(new Move(GeneralPurposeRegister.A0, lowerValue(inst.dst)));
        return null;
    }

    private static int getResult(
        TacBinaryOperation tacBinaryOperation,
        TacIntConstant tacLhsIntConstant,
        TacIntConstant tacRhsIntConstant) {
        int lhs = tacLhsIntConstant.value;
        int rhs = tacRhsIntConstant.value;
        return switch (tacBinaryOperation.op) {
            case ADD -> lhs + rhs;
            case SUBTRACT -> lhs - rhs;
            case MULTIPLY -> lhs * rhs;
            case DIVIDE -> lhs / rhs;
            case MODULO -> lhs % rhs;
            case LEFT_SHIFT -> lhs << rhs;
            case RIGHT_SHIFT -> lhs >> rhs;
            case BITWISE_AND -> lhs & rhs;
            case BITWISE_OR -> lhs | rhs;
            case BITWISE_XOR -> lhs ^ rhs;
            case LOGICAL_AND, LOGICAL_OR -> throw new IllegalStateException(
                "Should not have logical operators here, should be replaced to short-cut evaluation before");
            case EQUAL -> lhs == rhs ? 1 : 0;
            case NOT_EQUAL -> lhs != rhs ? 1 : 0;
            case LESS_THAN -> lhs < rhs ? 1 : 0;
            case LESS_OR_EQUAL -> lhs <= rhs ? 1 : 0;
            case GREATER_THAN -> lhs > rhs ? 1 : 0;
            case GREATER_OR_EQUAL -> lhs >= rhs ? 1 : 0;
        };
    }

    private static HighLevelOperand lowerValue(TacValue tacValue) {
        if (tacValue instanceof TacIntConstant tacIntConstant) {
            return new Immediate(tacIntConstant.value);
        } else if (tacValue instanceof TacVariable tacVariable) {
            return new Pseudo(tacVariable.identifier);
        }
        throw new UnsupportedOperationException("Unsupported value type: " + tacValue.getClass().getSimpleName());
    }
}
