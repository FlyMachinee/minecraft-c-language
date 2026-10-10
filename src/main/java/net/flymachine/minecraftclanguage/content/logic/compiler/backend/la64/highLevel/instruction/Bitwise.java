package net.flymachine.minecraftclanguage.content.logic.compiler.backend.la64.highLevel.instruction;

import net.flymachine.minecraftclanguage.content.logic.compiler.backend.la64.highLevel.AsmType;
import net.flymachine.minecraftclanguage.content.logic.compiler.backend.la64.highLevel.operand.HighLevelOperand;
import net.flymachine.minecraftclanguage.content.logic.compiler.common.BinaryOperator;

/**
 * 如有需要，会将 lhs 加载至 T0，rhs 加载至 T1，结果存放在 T0
 * <p>
 * 可能使用的临时寄存器：T0 T1
 */
public class Bitwise implements HighLevelInstruction {
    public final Operator op;
    public final AsmType asmType;
    /**
     * 如有需要，会将 lhs 加载至 T0
     */
    public HighLevelOperand lhs;
    /**
     * 如有需要，会将 rhs 加载至 T1
     */
    public HighLevelOperand rhs;
    /**
     * 如有需要，会将结果存放在 T0，然后再存放到 dst
     */
    public HighLevelOperand dst;

    public enum Operator {
        AND,
        OR,
        NOR,
        XOR,
        ANDN,
        ORN;

        public String mnemonic() {
            return switch (this) {
                case AND -> "and";
                case OR -> "or";
                case NOR -> "nor";
                case XOR -> "xor";
                case ANDN -> "andn";
                case ORN -> "orn";
            };
        }
    }

    public Bitwise(
        Operator op, AsmType asmType, HighLevelOperand lhs, HighLevelOperand rhs, HighLevelOperand dst) {

        if (asmType == AsmType.DOUBLE) {
            throw new IllegalArgumentException("Bitwise does not support DOUBLE type");
        }
        this.op = op;
        this.asmType = asmType;
        this.lhs = lhs;
        this.rhs = rhs;
        this.dst = dst;
    }

    public Bitwise(
        BinaryOperator op, AsmType asmType, HighLevelOperand lhs, HighLevelOperand rhs, HighLevelOperand dst) {

        if (asmType == AsmType.DOUBLE) {
            throw new IllegalArgumentException("Bitwise does not support DOUBLE type");
        }
        this.op = switch (op) {
            case ADD, SUBTRACT, MULTIPLY -> throw new IllegalArgumentException(
                "Use Binary instruction for addition, subtraction, and multiplication");
            case DIVIDE -> throw new IllegalArgumentException("Use DivOrMod instruction for division");
            case MODULO -> throw new IllegalArgumentException("Use Mod instruction for modulo");
            case LEFT_SHIFT, RIGHT_SHIFT ->
                throw new IllegalArgumentException("Use BitwiseShift instruction for bitwise shifts");
            case LESS_THAN, GREATER_THAN, LESS_OR_EQUAL, GREATER_OR_EQUAL, EQUAL, NOT_EQUAL ->
                throw new IllegalArgumentException("Use Compare instruction for comparisons");
            case BITWISE_AND -> Operator.AND;
            case BITWISE_OR -> Operator.OR;
            case BITWISE_XOR -> Operator.XOR;
            case LOGICAL_AND, LOGICAL_OR -> throw new IllegalArgumentException(
                "Should be lowered to branches, not implemented as a single instruction");
        };
        this.asmType = asmType;
        this.lhs = lhs;
        this.rhs = rhs;
        this.dst = dst;
    }

    @Override
    public <T> T accept(HighLevelVisitor<T> visitor) {
        return visitor.visit(this);
    }

}
