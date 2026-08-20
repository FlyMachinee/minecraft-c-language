package net.flymachine.minecraftclanguage.content.logic.compiler.common.constant;

import net.flymachine.minecraftclanguage.content.logic.compiler.backend.la64.highLevel.AsmType;
import net.flymachine.minecraftclanguage.content.logic.compiler.common.BinaryOperator;
import net.flymachine.minecraftclanguage.content.logic.compiler.common.Comparison;
import net.flymachine.minecraftclanguage.content.logic.compiler.common.UnaryOperator;
import net.flymachine.minecraftclanguage.content.logic.compiler.common.staticInit.LongInit;
import net.flymachine.minecraftclanguage.content.logic.compiler.common.type.BasicType;
import net.flymachine.minecraftclanguage.content.logic.compiler.common.type.Type;
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
    public ConstantDouble toDouble() {
        return new ConstantDouble(value);
    }

    @Override
    public ConstantPointer toPointer(Type referencedType) {
        return new ConstantPointer(value, referencedType);
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
        BasicType lhsType = getType();
        Type rhsType = rhs.getType();

        return switch (op) {
            case ADD, SUBTRACT, MULTIPLY, DIVIDE -> {
                if (rhsType.isArithmetic()) {
                    if (lhsType != rhsType) {
                        throw new UnsupportedOperationException("Cast to their common real type first");
                    }
                    yield apply(op, (ConstantLong) rhs);
                }
                throw new UnsupportedOperationException("Unsupported operation");
            }
            case MODULO, BITWISE_AND, BITWISE_OR, BITWISE_XOR -> {
                if (rhsType.isInteger()) {
                    if (lhsType != rhsType) {
                        throw new UnsupportedOperationException("Cast to their common real type first");
                    }
                    yield apply(op, (ConstantLong) rhs);
                }
                throw new UnsupportedOperationException("Unsupported operation");
            }
            case LEFT_SHIFT, RIGHT_SHIFT -> {
                if (rhsType.isInteger()) {
                    yield apply(op, rhs.toLong());
                }
                throw new UnsupportedOperationException("Unsupported operation");
            }
            case LOGICAL_AND, LOGICAL_OR -> {
                if (rhsType.isScalar()) {
                    if (op == BinaryOperator.LOGICAL_AND) {
                        yield new ConstantInt(this.isZero() || rhs.isZero() ? 0 : 1);
                    } else {
                        yield new ConstantInt(this.isZero() && rhs.isZero() ? 0 : 1);
                    }
                }
                throw new UnsupportedOperationException("Unsupported operation");
            }
            case LESS_THAN, LESS_OR_EQUAL, GREATER_THAN, GREATER_OR_EQUAL -> {
                if (!rhsType.isReal()) {
                    if (lhsType != rhsType) {
                        throw new UnsupportedOperationException("Cast to their common real type first");
                    }
                    yield apply(op, (ConstantLong) rhs);
                }
                throw new UnsupportedOperationException("Unsupported operation");
            }
            case EQUAL, NOT_EQUAL -> {
                if (!rhsType.isArithmetic()) {
                    if (lhsType != rhsType) {
                        throw new UnsupportedOperationException("Cast to their common real type first");
                    }
                    yield apply(op, (ConstantLong) rhs);
                }
                throw new UnsupportedOperationException("Unsupported operation");
            }
        };
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
            case LOGICAL_AND, LOGICAL_OR -> throw new UnsupportedOperationException("Should be handled earlier");
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

    @Override
    public boolean isNullPointer() {
        return value == 0;
    }
}