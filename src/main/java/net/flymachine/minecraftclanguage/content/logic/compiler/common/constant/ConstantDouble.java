package net.flymachine.minecraftclanguage.content.logic.compiler.common.constant;

import net.flymachine.minecraftclanguage.content.logic.compiler.backend.la64.highLevel.AsmType;
import net.flymachine.minecraftclanguage.content.logic.compiler.common.BinaryOperator;
import net.flymachine.minecraftclanguage.content.logic.compiler.common.Comparison;
import net.flymachine.minecraftclanguage.content.logic.compiler.common.UnaryOperator;
import net.flymachine.minecraftclanguage.content.logic.compiler.common.staticInit.DoubleInit;
import net.flymachine.minecraftclanguage.content.logic.compiler.common.type.BasicType;
import org.jetbrains.annotations.NotNull;

public record ConstantDouble(double value) implements Constant {

    public static final ConstantDouble ZERO = new ConstantDouble(0.0);
    public static final ConstantDouble ONE = new ConstantDouble(1.0);

    @Override
    public @NotNull String toString() {
        return Double.toString(value);
    }

    @Override
    public ConstantInt toInt() {
        return new ConstantInt((int) value);
    }

    @Override
    public ConstantLong toLong() {
        return new ConstantLong((long) value);
    }

    @Override
    public ConstantUnsignedInt toUnsignedInt() {
        return new ConstantUnsignedInt((int) (long) value);
    }

    @Override
    public ConstantUnsignedLong toUnsignedLong() {
        // 0x1.0p63 = 2^63
        if (value >= 0x1.0p63) {
            return new ConstantUnsignedLong((long) (value - 0x1.0p63) + Long.MIN_VALUE);
        }
        return new ConstantUnsignedLong((long) value);
    }

    @Override
    public ConstantDouble toDouble() {
        return this;
    }

    @Override
    public DoubleInit toStaticInit() {
        return new DoubleInit(value);
    }

    @Override
    public Constant apply(UnaryOperator op) {
        return switch (op) {
            case NEGATE -> new ConstantDouble(-value);
            case NOT -> new ConstantInt(value == 0.0 ? 1 : 0);
            case COMPLEMENT ->
                throw new UnsupportedOperationException("Bitwise complement is not supported for double constants");
        };
    }

    @Override
    public Constant apply(BinaryOperator op, Constant rhs) {
        return apply(op, rhs.toDouble());
    }

    @Override
    public ConstantInt apply(Comparison cmp, Constant rhs) {
        return apply(cmp, rhs.toDouble());
    }

    @Override
    public boolean isZero() {
        return value == 0.0;
    }

    @Override
    public BasicType getType() {
        return BasicType.DOUBLE;
    }

    @Override
    public AsmType getAsmType() {
        return AsmType.DOUBLE;
    }

    public Constant apply(BinaryOperator op, ConstantDouble rhs) {
        return switch (op) {
            case ADD -> new ConstantDouble(value + rhs.value);
            case SUBTRACT -> new ConstantDouble(value - rhs.value);
            case MULTIPLY -> new ConstantDouble(value * rhs.value);
            case DIVIDE -> new ConstantDouble(value / rhs.value);
            case MODULO ->
                throw new UnsupportedOperationException("Modulo operation is not supported for double constants");
            case LEFT_SHIFT, RIGHT_SHIFT, BITWISE_AND, BITWISE_OR, BITWISE_XOR ->
                throw new UnsupportedOperationException("Bitwise operation is not supported for double constants");
            case LOGICAL_AND -> new ConstantInt((value != 0.0 && rhs.value != 0.0) ? 1 : 0);
            case LOGICAL_OR -> new ConstantInt((value != 0.0 || rhs.value != 0.0) ? 1 : 0);
            case LESS_THAN -> new ConstantInt(value < rhs.value ? 1 : 0);
            case GREATER_THAN -> new ConstantInt(value > rhs.value ? 1 : 0);
            case EQUAL -> new ConstantInt(value == rhs.value ? 1 : 0);
            case NOT_EQUAL -> new ConstantInt(value != rhs.value ? 1 : 0);
            case LESS_OR_EQUAL -> new ConstantInt(value <= rhs.value ? 1 : 0);
            case GREATER_OR_EQUAL -> new ConstantInt(value >= rhs.value ? 1 : 0);
        };
    }

    public ConstantInt apply(Comparison cmp, ConstantDouble rhs) {
        return (ConstantInt) apply(cmp.toBinaryOperator(), rhs);
    }

}
