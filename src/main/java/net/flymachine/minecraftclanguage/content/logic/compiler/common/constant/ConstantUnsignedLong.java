package net.flymachine.minecraftclanguage.content.logic.compiler.common.constant;

import net.flymachine.minecraftclanguage.content.logic.compiler.backend.la64.highLevel.AsmType;
import net.flymachine.minecraftclanguage.content.logic.compiler.common.BinaryOperator;
import net.flymachine.minecraftclanguage.content.logic.compiler.common.Comparison;
import net.flymachine.minecraftclanguage.content.logic.compiler.common.UnaryOperator;
import net.flymachine.minecraftclanguage.content.logic.compiler.common.staticInit.UnsignedLongInit;
import net.flymachine.minecraftclanguage.content.logic.compiler.common.type.BasicType;
import net.flymachine.minecraftclanguage.content.logic.compiler.common.type.Type;
import org.jetbrains.annotations.NotNull;

public record ConstantUnsignedLong(long value) implements Constant {

    public static final ConstantUnsignedLong ZERO = new ConstantUnsignedLong(0);
    public static final ConstantUnsignedLong ONE = new ConstantUnsignedLong(1);

    @Override
    public @NotNull String toString() {
        return Long.toUnsignedString(value) + "ULL";
    }

    @Override
    public ConstantInt toInt() {
        return new ConstantInt((int) value);
    }

    @Override
    public ConstantLong toLong() {
        return new ConstantLong(value);
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
    public UnsignedLongInit toStaticInit() {
        return new UnsignedLongInit(value);
    }

    @Override
    public Constant apply(UnaryOperator op) {
        return switch (op) {
            case NEGATE -> new ConstantUnsignedLong(-value);
            case NOT -> new ConstantInt(value == 0 ? 1 : 0);
            case COMPLEMENT -> new ConstantUnsignedLong(~value);
        };
    }

    @Override
    public Constant apply(BinaryOperator op, Constant rhs) {
        if (rhs instanceof ConstantInt || rhs instanceof ConstantLong || rhs instanceof ConstantUnsignedInt) {
            return apply(op, rhs.toUnsignedLong());
        } else if (rhs instanceof ConstantUnsignedLong unsignedLongRhs) {
            return apply(op, unsignedLongRhs);
        }
        throw new IllegalStateException("Unsupported constant type: " + rhs.getClass());
    }

    @Override
    public ConstantInt apply(Comparison cmp, Constant rhs) {
        if (rhs instanceof ConstantInt || rhs instanceof ConstantLong || rhs instanceof ConstantUnsignedInt) {
            return apply(cmp, rhs.toUnsignedLong());
        } else if (rhs instanceof ConstantUnsignedLong unsignedLongRhs) {
            return apply(cmp, unsignedLongRhs);
        }
        throw new IllegalStateException("Unsupported constant type: " + rhs.getClass());
    }

    @Override
    public boolean isZero() {
        return false;
    }

    @Override
    public Type getType() {
        return BasicType.UNSIGNED_LONG;
    }

    @Override
    public AsmType getAsmType() {
        return AsmType.DWORD;
    }

    public Constant apply(BinaryOperator op, ConstantUnsignedLong rhs) {
        return switch (op) {
            case ADD -> new ConstantUnsignedLong(value + rhs.value);
            case SUBTRACT -> new ConstantUnsignedLong(value - rhs.value);
            case MULTIPLY -> new ConstantUnsignedLong(value * rhs.value);
            case DIVIDE -> new ConstantUnsignedLong(rhs.value == 0 ? 0 : Long.divideUnsigned(value, rhs.value));
            case MODULO -> new ConstantUnsignedLong(rhs.value == 0 ? 0 : Long.remainderUnsigned(value, rhs.value));
            case LEFT_SHIFT -> new ConstantUnsignedLong(value << rhs.value);
            case RIGHT_SHIFT -> new ConstantUnsignedLong(value >>> rhs.value);
            case BITWISE_AND -> new ConstantUnsignedLong(value & rhs.value);
            case BITWISE_OR -> new ConstantUnsignedLong(value | rhs.value);
            case BITWISE_XOR -> new ConstantUnsignedLong(value ^ rhs.value);
            case LOGICAL_AND -> new ConstantInt((value != 0 && rhs.value != 0) ? 1 : 0);
            case LOGICAL_OR -> new ConstantInt((value != 0 || rhs.value != 0) ? 1 : 0);
            case LESS_THAN -> new ConstantInt(Long.compareUnsigned(value, rhs.value) < 0 ? 1 : 0);
            case GREATER_THAN -> new ConstantInt(Long.compareUnsigned(value, rhs.value) > 0 ? 1 : 0);
            case EQUAL -> new ConstantInt(value == rhs.value ? 1 : 0);
            case NOT_EQUAL -> new ConstantInt(value != rhs.value ? 1 : 0);
            case LESS_OR_EQUAL -> new ConstantInt(Long.compareUnsigned(value, rhs.value) <= 0 ? 1 : 0);
            case GREATER_OR_EQUAL -> new ConstantInt(Long.compareUnsigned(value, rhs.value) >= 0 ? 1 : 0);
        };
    }

    public ConstantInt apply(Comparison cmp, ConstantUnsignedLong rhs) {
        return (ConstantInt) apply(cmp.toBinaryOperator(), rhs);
    }
}
