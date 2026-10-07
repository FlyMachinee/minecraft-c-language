package net.flymachine.minecraftclanguage.content.logic.compiler.common.constant;

import com.mojang.datafixers.util.Either;
import net.flymachine.minecraftclanguage.content.logic.compiler.backend.la64.highLevel.AsmType;
import net.flymachine.minecraftclanguage.content.logic.compiler.common.BinaryOperator;
import net.flymachine.minecraftclanguage.content.logic.compiler.common.Comparison;
import net.flymachine.minecraftclanguage.content.logic.compiler.common.UnaryOperator;
import net.flymachine.minecraftclanguage.content.logic.compiler.common.staticInit.UnsignedLongInit;
import net.flymachine.minecraftclanguage.content.logic.compiler.common.type.BasicType;
import net.flymachine.minecraftclanguage.content.logic.compiler.common.type.Type;
import net.flymachine.minecraftclanguage.content.logic.compiler.common.util.UndefinedBehaviourUtil;
import net.flymachine.minecraftclanguage.content.logic.errorHandle.DiagnosticReporter;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

public record ConstantUnsignedLong(long value) implements IntegerConstant {

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
    public ConstantDouble toDouble() {
        if (value >= 0) {
            return new ConstantDouble((double) value);
        } else {
            long lowBits = value & Long.MAX_VALUE;
            double result = Math.scalb(1.0, 63) + (double) lowBits;
            return new ConstantDouble(result);
        }
    }

    @Override
    public ConstantChar toChar() {
        return new ConstantChar((byte) value);
    }

    @Override
    public ConstantUnsignedChar toUnsignedChar() {
        return new ConstantUnsignedChar((byte) value);
    }

    @Override
    public long toByteRepresentation() {
        return value;
    }

    @Override
    public ConstantPointer toPointer(Type referencedType) {
        return new ConstantPointer(value, referencedType);
    }

    @Override
    public UnsignedLongInit toStaticInit() {
        return new UnsignedLongInit(value);
    }

    @Override
    public Constant apply(UnaryOperator op) {
        return switch (op) {
            case POSITIVE -> this;
            case NEGATE -> new ConstantUnsignedLong(-value);
            case NOT -> new ConstantInt(value == 0 ? 1 : 0);
            case COMPLEMENT -> new ConstantUnsignedLong(~value);
        };
    }

    @Override
    public Constant apply(BinaryOperator op, Constant rhs) {
        BasicType lhsType = getType();
        Type rhsType = rhs.getType();

        return switch (op) {
            case ADD, SUBTRACT, MULTIPLY, DIVIDE, EQUAL, NOT_EQUAL -> {
                if (rhsType.isArithmetic()) {
                    if (!lhsType.isCompatible(rhsType)) {
                        throw new UnsupportedOperationException("Cast to their common real type first");
                    }
                    yield apply(op, (ConstantUnsignedLong) rhs);
                }
                throw new UnsupportedOperationException("Unsupported operation");
            }
            case MODULO, BITWISE_AND, BITWISE_OR, BITWISE_XOR -> {
                if (rhsType.isInteger()) {
                    if (!lhsType.isCompatible(rhsType)) {
                        throw new UnsupportedOperationException("Cast to their common real type first");
                    }
                    yield apply(op, (ConstantUnsignedLong) rhs);
                }
                throw new UnsupportedOperationException("Unsupported operation");
            }
            case LEFT_SHIFT, RIGHT_SHIFT -> {
                if (rhsType.isInteger()) {
                    yield apply(op, rhs.toUnsignedLong());
                }
                throw new UnsupportedOperationException("Unsupported operation");
            }
            case LOGICAL_AND, LOGICAL_OR -> throw new UnsupportedOperationException("Should be handled earlier");
            case LESS_THAN, LESS_OR_EQUAL, GREATER_THAN, GREATER_OR_EQUAL -> {
                if (rhsType.isReal()) {
                    if (!lhsType.isCompatible(rhsType)) {
                        throw new UnsupportedOperationException("Cast to their common real type first");
                    }
                    yield apply(op, (ConstantUnsignedLong) rhs);
                }
                throw new UnsupportedOperationException("Unsupported operation");
            }
        };
    }

    @Override
    public Either<Constant, String> tryApply(UnaryOperator op, DiagnosticReporter reporter) {
        return Either.left(apply(op));
    }

    @Override
    public Either<Constant, String> tryApply(BinaryOperator op, Constant rhs, DiagnosticReporter reporter) {
        BasicType lhsType = getType();
        Type rhsType = rhs.getType();

        return switch (op) {
            case ADD, SUBTRACT, MULTIPLY, DIVIDE, EQUAL, NOT_EQUAL -> {
                if (rhsType.isArithmetic()) {
                    if (!lhsType.isCompatible(rhsType)) {
                        throw new UnsupportedOperationException("Cast to their common real type first");
                    }
                    yield tryApply(op, (ConstantUnsignedLong) rhs, reporter);
                }
                throw new UnsupportedOperationException("Unsupported operation");
            }
            case MODULO, BITWISE_AND, BITWISE_OR, BITWISE_XOR -> {
                if (rhsType.isInteger()) {
                    if (!lhsType.isCompatible(rhsType)) {
                        throw new UnsupportedOperationException("Cast to their common real type first");
                    }
                    yield tryApply(op, (ConstantUnsignedLong) rhs, reporter);
                }
                throw new UnsupportedOperationException("Unsupported operation");
            }
            case LEFT_SHIFT, RIGHT_SHIFT -> {
                if (rhsType.isInteger()) {
                    Optional<String> checkRes =
                        UndefinedBehaviourUtil.checkBitwiseShift(
                            op == BinaryOperator.LEFT_SHIFT, this.asNode(), rhs.asNode());
                    if (checkRes.isPresent()) {
                        yield Either.right(checkRes.get());
                    } else {
                        yield Either.left(apply(op, rhs.toUnsignedLong()));
                    }
                }
                throw new UnsupportedOperationException("Unsupported operation");
            }
            case LOGICAL_AND, LOGICAL_OR -> throw new UnsupportedOperationException("Should be handled earlier");
            case LESS_THAN, LESS_OR_EQUAL, GREATER_THAN, GREATER_OR_EQUAL -> {
                if (rhsType.isReal()) {
                    if (!lhsType.isCompatible(rhsType)) {
                        throw new UnsupportedOperationException("Cast to their common real type first");
                    }
                    yield Either.left(apply(op, rhs.toUnsignedLong()));
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
        return BasicType.UNSIGNED_LONG;
    }

    @Override
    public AsmType getAsmType() {
        return AsmType.DWORD;
    }

    private Constant apply(BinaryOperator op, ConstantUnsignedLong rhs) {
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
            case LOGICAL_AND, LOGICAL_OR -> throw new UnsupportedOperationException("Should be handled earlier");
            case LESS_THAN -> new ConstantInt(Long.compareUnsigned(value, rhs.value) < 0 ? 1 : 0);
            case GREATER_THAN -> new ConstantInt(Long.compareUnsigned(value, rhs.value) > 0 ? 1 : 0);
            case EQUAL -> new ConstantInt(value == rhs.value ? 1 : 0);
            case NOT_EQUAL -> new ConstantInt(value != rhs.value ? 1 : 0);
            case LESS_OR_EQUAL -> new ConstantInt(Long.compareUnsigned(value, rhs.value) <= 0 ? 1 : 0);
            case GREATER_OR_EQUAL -> new ConstantInt(Long.compareUnsigned(value, rhs.value) >= 0 ? 1 : 0);
        };
    }

    private Either<Constant, String> tryApply(
        BinaryOperator op, ConstantUnsignedLong rhs, DiagnosticReporter reporter) {
        if (op == BinaryOperator.DIVIDE || op == BinaryOperator.MODULO) {
            if (rhs.value == 0) {
                return Either.right("division by zero");
            }
        }
        return Either.left(apply(op, rhs));
    }

    public ConstantInt apply(Comparison cmp, ConstantUnsignedLong rhs) {
        return (ConstantInt) apply(cmp.toBinaryOperator(), rhs);
    }

    @Override
    public boolean isNullPointer() {
        return value == 0;
    }

    @Override
    public boolean isSigned() {
        return false;
    }

    @Override
    public boolean isPositive() {
        return value != 0;
    }

    @Override
    public boolean isNegative() {
        return false;
    }

    @Override
    public boolean isNonNegative() {
        return true;
    }
}
