package net.flymachine.minecraftclanguage.content.logic.compiler.common.constant;

import com.mojang.datafixers.util.Either;
import net.flymachine.minecraftclanguage.content.logic.compiler.backend.la64.highLevel.AsmType;
import net.flymachine.minecraftclanguage.content.logic.compiler.common.BinaryOperator;
import net.flymachine.minecraftclanguage.content.logic.compiler.common.Comparison;
import net.flymachine.minecraftclanguage.content.logic.compiler.common.UnaryOperator;
import net.flymachine.minecraftclanguage.content.logic.compiler.common.staticInit.LongInit;
import net.flymachine.minecraftclanguage.content.logic.compiler.common.type.BasicType;
import net.flymachine.minecraftclanguage.content.logic.compiler.common.type.Type;
import net.flymachine.minecraftclanguage.content.logic.compiler.common.util.UndefinedBehaviourUtil;
import net.flymachine.minecraftclanguage.content.logic.errorHandle.DiagnosticReporter;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

public record ConstantLong(long value) implements IntegerConstant {

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
            case ADD -> {
                if (rhsType.isArithmetic()) {
                    if (!lhsType.isCompatible(rhsType)) {
                        throw new UnsupportedOperationException("Cast to their common real type first");
                    }
                    yield apply(op, (ConstantLong) rhs);
                }
                if (rhs instanceof ConstantPointer rhsPtr) {
                    yield new ConstantPointer(
                        rhsPtr.value() + value * rhsPtr.referencedType().sizeof(),
                        rhsPtr.referencedType());
                }
                throw new UnsupportedOperationException("Unsupported operation");
            }
            case SUBTRACT, MULTIPLY, DIVIDE, EQUAL, NOT_EQUAL -> {
                if (rhsType.isArithmetic()) {
                    if (!lhsType.isCompatible(rhsType)) {
                        throw new UnsupportedOperationException("Cast to their common real type first");
                    }
                    yield apply(op, (ConstantLong) rhs);
                }
                throw new UnsupportedOperationException("Unsupported operation");
            }
            case MODULO, BITWISE_AND, BITWISE_OR, BITWISE_XOR -> {
                if (rhsType.isInteger()) {
                    if (!lhsType.isCompatible(rhsType)) {
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
            case LOGICAL_AND, LOGICAL_OR -> throw new UnsupportedOperationException("Should be handled earlier");
            case LESS_THAN, LESS_OR_EQUAL, GREATER_THAN, GREATER_OR_EQUAL -> {
                if (rhsType.isReal()) {
                    if (!lhsType.isCompatible(rhsType)) {
                        throw new UnsupportedOperationException("Cast to their common real type first");
                    }
                    yield apply(op, (ConstantLong) rhs);
                }
                throw new UnsupportedOperationException("Unsupported operation");
            }
        };
    }

    @Override
    public Either<Constant, String> tryApply(UnaryOperator op, DiagnosticReporter reporter) {
        if (op == UnaryOperator.NEGATE && value == Long.MIN_VALUE) {
            return Either.right(
                "integer overflow in expression of type '" + reporter.white("long") + "' results in '" +
                reporter.white("-9223372036854775808") + "'");
        }
        return Either.left(apply(op));
    }

    @Override
    public Either<Constant, String> tryApply(BinaryOperator op, Constant rhs, DiagnosticReporter reporter) {
        BasicType lhsType = getType();
        Type rhsType = rhs.getType();

        return switch (op) {
            case ADD -> {
                if (rhsType.isArithmetic()) {
                    if (!lhsType.isCompatible(rhsType)) {
                        throw new UnsupportedOperationException("Cast to their common real type first");
                    }
                    yield tryApply(op, (ConstantLong) rhs, reporter);
                }
                if (rhs instanceof ConstantPointer rhsPtr) {
                    yield Either.left(new ConstantPointer(
                        rhsPtr.value() + value * rhsPtr.referencedType().sizeof(), rhsPtr.referencedType()));
                }
                throw new UnsupportedOperationException("Unsupported operation");
            }
            case SUBTRACT, MULTIPLY, DIVIDE, EQUAL, NOT_EQUAL -> {
                if (rhsType.isArithmetic()) {
                    if (!lhsType.isCompatible(rhsType)) {
                        throw new UnsupportedOperationException("Cast to their common real type first");
                    }
                    yield tryApply(op, (ConstantLong) rhs, reporter);
                }
                throw new UnsupportedOperationException("Unsupported operation");
            }
            case MODULO, BITWISE_AND, BITWISE_OR, BITWISE_XOR -> {
                if (rhsType.isInteger()) {
                    if (!lhsType.isCompatible(rhsType)) {
                        throw new UnsupportedOperationException("Cast to their common real type first");
                    }
                    yield tryApply(op, (ConstantLong) rhs, reporter);
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
                        yield tryApply(op, rhs.toLong(), reporter);
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
                    yield Either.left(apply(op, (ConstantLong) rhs));
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

    private Constant apply(BinaryOperator op, ConstantLong rhs) {
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

    private Either<Constant, String> tryApply(BinaryOperator op, ConstantLong rhs, DiagnosticReporter reporter) {
        if (op == BinaryOperator.DIVIDE || op == BinaryOperator.MODULO) {
            if (rhs.value == 0) {
                return Either.right("division by zero");
            }
            if (value == Long.MIN_VALUE && rhs.value == -1) {
                return Either.right(
                    "integer overflow in expression of type '" + reporter.white("long") + "' results in '" +
                    reporter.white(op == BinaryOperator.DIVIDE ? "-9223372036854775808" : "0") + "'");
            }
        } else if (op == BinaryOperator.LEFT_SHIFT) {
            if (value > (Long.MAX_VALUE >> rhs.value)) {
                int bitsNeeded = (64 - Long.numberOfLeadingZeros(value)) + (int) rhs.value + 1;
                return Either.right(
                    "result of '" + reporter.white(value + " << " + rhs.value) + "' requires " + bitsNeeded
                    + " bits to represent, but '" + reporter.white("long") + "' only has 64 bits");
            }
        }

        try {
            return Either.left(switch (op) {
                case ADD -> new ConstantLong(Math.addExact(value, rhs.value));
                case SUBTRACT -> new ConstantLong(Math.subtractExact(value, rhs.value));
                case MULTIPLY -> new ConstantLong(Math.multiplyExact(value, rhs.value));
                default -> apply(op, rhs);
            });
        } catch (ArithmeticException e) {
            return Either.right(
                "integer overflow in expression of type '" + reporter.white("long") + "' results in '" +
                reporter.white(String.valueOf(((ConstantLong) apply(op, rhs)).value)) + "'");
        }
    }

    public ConstantInt apply(Comparison cmp, ConstantLong rhs) {
        return (ConstantInt) apply(cmp.toBinaryOperator(), rhs);
    }

    @Override
    public boolean isNullPointer() {
        return value == 0;
    }

    @Override
    public boolean isSigned() {
        return true;
    }

    @Override
    public boolean isPositive() {
        return value > 0;
    }
}