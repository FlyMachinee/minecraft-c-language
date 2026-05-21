package net.flymachine.minecraftclanguage.content.logic.compiler.common.constant;

import net.flymachine.minecraftclanguage.content.logic.compiler.backend.la64.highLevel.AsmType;
import net.flymachine.minecraftclanguage.content.logic.compiler.common.BinaryOperator;
import net.flymachine.minecraftclanguage.content.logic.compiler.common.Comparison;
import net.flymachine.minecraftclanguage.content.logic.compiler.common.UnaryOperator;
import net.flymachine.minecraftclanguage.content.logic.compiler.common.staticInit.LongInit;
import net.flymachine.minecraftclanguage.content.logic.compiler.common.type.BasicType;
import org.jetbrains.annotations.NotNull;

public record ConstantLong(long value) implements Constant {

    public static final ConstantLong ZERO = new ConstantLong(0);
    public static final ConstantLong ONE = new ConstantLong(1);

    @Override
    public @NotNull String toString() {
        return value + "LL";
    }

    @Override
    public ConstantInt toInt() {
        return new ConstantInt((int) value);
    }

    @Override
    public ConstantLong toLong() {
        return this;
    }

    @Override
    public ConstantUnsignedInt toUnsignedInt() {
        return new ConstantUnsignedInt((int) value);
    }

    @Override
    public ConstantUnsignedLong toUnsignedLong() {
        return new ConstantUnsignedLong(value);
    }

    @Override
    public LongInit toStaticInit() {
        return new LongInit(value);
    }

    @Override
    public Constant apply(UnaryOperator op) {
        return switch (op) {
            case NEGATE -> new ConstantLong(-value);
            case NOT -> new ConstantInt(value == 0 ? 1 : 0);
            case COMPLEMENT -> new ConstantLong(~value);
        };
    }

    @Override
    public Constant apply(BinaryOperator op, Constant rhs) {
        if (rhs instanceof ConstantInt || rhs instanceof ConstantUnsignedInt) {
            return apply(op, rhs.toLong());
        } else if (rhs instanceof ConstantLong longRhs) {
            return apply(op, longRhs);
        } else if (rhs instanceof ConstantUnsignedLong unsignedLongRhs) {
            return toUnsignedLong().apply(op, unsignedLongRhs);
        }
        throw new IllegalStateException("Unsupported constant type: " + rhs.getClass());
    }

    @Override
    public ConstantInt apply(Comparison cmp, Constant rhs) {
        if (rhs instanceof ConstantInt || rhs instanceof ConstantUnsignedInt) {
            return apply(cmp, rhs.toLong());
        } else if (rhs instanceof ConstantLong longRhs) {
            return apply(cmp, longRhs);
        } else if (rhs instanceof ConstantUnsignedLong unsignedLongRhs) {
            return toUnsignedLong().apply(cmp, unsignedLongRhs);
        }
        throw new IllegalStateException("Unsupported constant type: " + rhs.getClass());
    }

    @Override
    public boolean isZero() {
        return value == 0;
    }

    @Override
    public BasicType getType() {
        return BasicType.LONG;
    }

    @Override
    public AsmType getAsmType() {
        return AsmType.DWORD;
    }

    public Constant apply(BinaryOperator op, ConstantLong rhs) {
        return switch (op) {
            case ADD -> new ConstantLong(value + rhs.value);
            case SUBTRACT -> new ConstantLong(value - rhs.value);
            case MULTIPLY -> new ConstantLong(value * rhs.value);
            case DIVIDE -> new ConstantLong(rhs.value == 0 ? 0 : value / rhs.value);
            case MODULO -> new ConstantLong(rhs.value == 0 ? 0 : value % rhs.value);
            case LEFT_SHIFT -> new ConstantLong(value << rhs.value);
            case RIGHT_SHIFT -> new ConstantLong(value >> rhs.value);
            case BITWISE_AND -> new ConstantLong(value & rhs.value);
            case BITWISE_OR -> new ConstantLong(value | rhs.value);
            case BITWISE_XOR -> new ConstantLong(value ^ rhs.value);
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

    public ConstantInt apply(Comparison cmp, ConstantLong rhs) {
        return (ConstantInt) apply(cmp.toBinaryOperator(), rhs);
    }
}