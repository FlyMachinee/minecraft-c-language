package net.flymachine.minecraftclanguage.content.logic.compiler.common.constant;

import net.flymachine.minecraftclanguage.content.logic.compiler.backend.la64.highLevel.AsmType;
import net.flymachine.minecraftclanguage.content.logic.compiler.common.BinaryOperator;
import net.flymachine.minecraftclanguage.content.logic.compiler.common.Comparison;
import net.flymachine.minecraftclanguage.content.logic.compiler.common.UnaryOperator;
import net.flymachine.minecraftclanguage.content.logic.compiler.common.staticInit.IntInit;
import net.flymachine.minecraftclanguage.content.logic.compiler.common.type.BasicType;
import org.jetbrains.annotations.NotNull;

public record ConstantInt(int value) implements Constant {

    public static final ConstantInt ZERO = new ConstantInt(0);
    public static final ConstantInt ONE = new ConstantInt(1);

    @Override
    public @NotNull String toString() {
        return Integer.toString(value);
    }

    @Override
    public ConstantInt toInt() {
        return this;
    }

    @Override
    public ConstantLong toLong() {
        return new ConstantLong(value);
    }

    @Override
    public ConstantUnsignedInt toUnsignedInt() {
        return new ConstantUnsignedInt(value);
    }

    @Override
    public ConstantUnsignedLong toUnsignedLong() {
        return new ConstantUnsignedLong(value);
    }

    @Override
    public ConstantDouble toDouble() {
        return new ConstantDouble(value);
    }

    @Override
    public IntInit toStaticInit() {
        return new IntInit(value);
    }

    @Override
    public ConstantInt apply(UnaryOperator op) {
        return switch (op) {
            case NEGATE -> new ConstantInt(-value);
            case NOT -> new ConstantInt(value == 0 ? 1 : 0);
            case COMPLEMENT -> new ConstantInt(~value);
        };
    }

    @Override
    public Constant apply(BinaryOperator op, Constant rhs) {
        if (rhs instanceof ConstantInt intRhs) {
            return apply(op, intRhs);
        } else if (rhs instanceof ConstantLong longRhs) {
            return toLong().apply(op, longRhs);
        } else if (rhs instanceof ConstantUnsignedInt unsignedIntRhs) {
            return toUnsignedInt().apply(op, unsignedIntRhs);
        } else if (rhs instanceof ConstantUnsignedLong unsignedLongRhs) {
            return toUnsignedLong().apply(op, unsignedLongRhs);
        } else if (rhs instanceof ConstantDouble doubleRhs) {
            return toDouble().apply(op, doubleRhs);
        }
        throw new IllegalStateException("Unsupported constant type: " + rhs.getClass());
    }

    @Override
    public ConstantInt apply(Comparison cmp, Constant rhs) {
        return (ConstantInt) apply(cmp.toBinaryOperator(), rhs);
    }

    @Override
    public boolean isZero() {
        return value == 0;
    }

    @Override
    public BasicType getType() {
        return BasicType.INT;
    }

    @Override
    public AsmType getAsmType() {
        return AsmType.WORD;
    }

    public ConstantInt apply(BinaryOperator op, ConstantInt rhs) {
        return switch (op) {
            case ADD -> new ConstantInt(value + rhs.value);
            case SUBTRACT -> new ConstantInt(value - rhs.value);
            case MULTIPLY -> new ConstantInt(value * rhs.value);
            case DIVIDE -> new ConstantInt(rhs.value == 0 ? 0 : value / rhs.value);
            case MODULO -> new ConstantInt(rhs.value == 0 ? 0 : value % rhs.value);
            case LEFT_SHIFT -> new ConstantInt(value << rhs.value);
            case RIGHT_SHIFT -> new ConstantInt(value >> rhs.value);
            case BITWISE_AND -> new ConstantInt(value & rhs.value);
            case BITWISE_OR -> new ConstantInt(value | rhs.value);
            case BITWISE_XOR -> new ConstantInt(value ^ rhs.value);
            case LOGICAL_AND -> new ConstantInt((value != 0 && rhs.value != 0) ? 1 : 0);
            case LOGICAL_OR -> new ConstantInt((value != 0 || rhs.value != 0) ? 1 : 0);
            case LESS_THAN -> new ConstantInt(value < rhs.value ? 1 : 0);
            case GREATER_THAN -> new ConstantInt(value > rhs.value ? 1 : 0);
            case EQUAL -> new ConstantInt(value == rhs.value ? 1 : 0);
            case NOT_EQUAL -> new ConstantInt(value != rhs.value ? 1 : 0);
            case LESS_OR_EQUAL -> new ConstantInt(value <= rhs.value ? 1 : 0);
            case GREATER_OR_EQUAL -> new ConstantInt(value >= rhs.value ? 1 : 0);
        };
    }

    public ConstantInt apply(Comparison cmp, ConstantInt rhs) {
        return apply(cmp.toBinaryOperator(), rhs);
    }
}
