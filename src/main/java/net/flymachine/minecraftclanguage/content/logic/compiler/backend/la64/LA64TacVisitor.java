package net.flymachine.minecraftclanguage.content.logic.compiler.backend.la64;

import net.flymachine.minecraftclanguage.content.logic.architecture.la64.register.GeneralPurposeRegister;
import net.flymachine.minecraftclanguage.content.logic.compiler.backend.la64.highLevel.instruction.*;
import net.flymachine.minecraftclanguage.content.logic.compiler.backend.la64.highLevel.operand.HighLevelOperand;
import net.flymachine.minecraftclanguage.content.logic.compiler.backend.la64.highLevel.operand.Immediate;
import net.flymachine.minecraftclanguage.content.logic.compiler.backend.la64.highLevel.operand.Pseudo;
import net.flymachine.minecraftclanguage.content.logic.compiler.common.BinaryOperator;
import net.flymachine.minecraftclanguage.content.logic.compiler.ir.*;

import java.util.ArrayList;
import java.util.List;

public class LA64TacVisitor implements TacVisitor<Void> {

    private final List<HighLevelInstruction> target = new ArrayList<>();

    public LA64TacVisitor() { }

    public List<HighLevelInstruction> getTarget() {
        return target;
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
