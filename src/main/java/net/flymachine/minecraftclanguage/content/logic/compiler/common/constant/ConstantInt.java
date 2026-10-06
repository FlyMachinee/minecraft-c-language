package net.flymachine.minecraftclanguage.content.logic.compiler.common.constant;

import com.mojang.datafixers.util.Either;
import net.flymachine.minecraftclanguage.content.logic.compiler.backend.la64.highLevel.AsmType;
import net.flymachine.minecraftclanguage.content.logic.compiler.common.BinaryOperator;
import net.flymachine.minecraftclanguage.content.logic.compiler.common.Comparison;
import net.flymachine.minecraftclanguage.content.logic.compiler.common.UnaryOperator;
import net.flymachine.minecraftclanguage.content.logic.compiler.common.staticInit.IntInit;
import net.flymachine.minecraftclanguage.content.logic.compiler.common.type.BasicType;
import net.flymachine.minecraftclanguage.content.logic.compiler.common.type.Type;
import net.flymachine.minecraftclanguage.content.logic.compiler.common.util.UndefinedBehaviourUtil;
import net.flymachine.minecraftclanguage.content.logic.errorHandle.DiagnosticReporter;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

public record ConstantInt(int value) implements IntegerConstant {

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
    public ConstantChar toChar() {
        return new ConstantChar((byte) value);
    }

    @Override
    public ConstantUnsignedChar toUnsignedChar() {
        return new ConstantUnsignedChar((byte) value);
    }

    @Override
    public ConstantPointer toPointer(Type referencedType) {
        return new ConstantPointer(value, referencedType);
    }

    @Override
    public IntInit toStaticInit() {
        return new IntInit(value);
    }

    @Override
    public ConstantInt apply(UnaryOperator op) {
        return switch (op) {
            case POSITIVE -> this;
            case NEGATE -> new ConstantInt(-value);
            case NOT -> new ConstantInt(value == 0 ? 1 : 0);
            case COMPLEMENT -> new ConstantInt(~value);
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
                    yield apply(op, (ConstantInt) rhs);
                }
                throw new UnsupportedOperationException("Unsupported operation");
            }
            case MODULO, BITWISE_AND, BITWISE_OR, BITWISE_XOR -> {
                if (rhsType.isInteger()) {
                    if (lhsType.isCompatible(rhsType)) {
                        throw new UnsupportedOperationException("Cast to their common real type first");
                    }
                    yield apply(op, (ConstantInt) rhs);
                }
                throw new UnsupportedOperationException("Unsupported operation");
            }
            case LEFT_SHIFT, RIGHT_SHIFT -> {
                if (rhsType.isInteger()) {
                    yield apply(op, rhs.toInt());
                }
                throw new UnsupportedOperationException("Unsupported operation");
            }
            case LOGICAL_AND, LOGICAL_OR -> throw new UnsupportedOperationException("Should be handled earlier");
            case LESS_THAN, LESS_OR_EQUAL, GREATER_THAN, GREATER_OR_EQUAL -> {
                if (rhsType.isReal()) {
                    if (!lhsType.isCompatible(rhsType)) {
                        throw new UnsupportedOperationException("Cast to their common real type first");
                    }
                    yield apply(op, (ConstantInt) rhs);
                }
                throw new UnsupportedOperationException("Unsupported operation");
            }
        };
    }

    @Override
    public Either<Constant, String> tryApply(UnaryOperator op, DiagnosticReporter reporter) {
        if (op == UnaryOperator.NEGATE && value == Integer.MIN_VALUE) {
            return Either.right(
                "integer overflow in expression of type '" + reporter.white("int") + "' results in '" +
                reporter.white("-2147483648") + "'");
        }
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
                    yield tryApply(op, (ConstantInt) rhs, reporter);
                }
                throw new UnsupportedOperationException("Unsupported operation");
            }
            case MODULO, BITWISE_AND, BITWISE_OR, BITWISE_XOR -> {
                if (rhsType.isInteger()) {
                    if (!lhsType.isCompatible(rhsType)) {
                        throw new UnsupportedOperationException("Cast to their common real type first");
                    }
                    yield tryApply(op, (ConstantInt) rhs, reporter);
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
                        yield tryApply(op, rhs.toInt(), reporter);
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
                    yield Either.left(apply(op, (ConstantInt) rhs));
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
        return BasicType.INT;
    }

    @Override
    public AsmType getAsmType() {
        return AsmType.WORD;
    }

    private ConstantInt apply(BinaryOperator op, ConstantInt rhs) {
        return new ConstantInt(apply(op, rhs.value));
    }

    private int apply(BinaryOperator op, int rhs) {
        return switch (op) {
            case ADD -> value + rhs;
            case SUBTRACT -> value - rhs;
            case MULTIPLY -> value * rhs;
            case DIVIDE -> rhs == 0 ? 0 : value / rhs;
            case MODULO -> rhs == 0 ? 0 : value % rhs;
            case LEFT_SHIFT -> value << rhs;
            case RIGHT_SHIFT -> value >> rhs;
            case BITWISE_AND -> value & rhs;
            case BITWISE_OR -> value | rhs;
            case BITWISE_XOR -> value ^ rhs;
            case LOGICAL_AND, LOGICAL_OR -> throw new UnsupportedOperationException("Should be handled earlier");
            case LESS_THAN -> value < rhs ? 1 : 0;
            case GREATER_THAN -> value > rhs ? 1 : 0;
            case EQUAL -> value == rhs ? 1 : 0;
            case NOT_EQUAL -> value != rhs ? 1 : 0;
            case LESS_OR_EQUAL -> value <= rhs ? 1 : 0;
            case GREATER_OR_EQUAL -> value >= rhs ? 1 : 0;
        };
    }

    private Either<Constant, String> tryApply(BinaryOperator op, ConstantInt rhs, DiagnosticReporter reporter) {
        if (op == BinaryOperator.DIVIDE || op == BinaryOperator.MODULO) {
            if (rhs.value == 0) {
                return Either.right("division by zero");
            }
            if (value == Integer.MIN_VALUE && rhs.value == -1) {
                return Either.right(
                    "integer overflow in expression of type '" + reporter.white("int") + "' results in '" +
                    reporter.white(op == BinaryOperator.DIVIDE ? "-2147483648" : "0") + "'");
            }
        } else if (op == BinaryOperator.LEFT_SHIFT) {
            if (value > (Integer.MAX_VALUE >> rhs.value)) {
                int bitsNeeded = (32 - Integer.numberOfLeadingZeros(value)) + rhs.value + 1;
                return Either.right(
                    "result of '" + reporter.white(value + " << " + rhs.value) + "' requires " + bitsNeeded
                    + " bits to represent, but '" + reporter.white("int") + "' only has 32 bits");
            }
        }

        try {
            return Either.left(new ConstantInt(switch (op) {
                case ADD -> Math.addExact(value, rhs.value);
                case SUBTRACT -> Math.subtractExact(value, rhs.value);
                case MULTIPLY -> Math.multiplyExact(value, rhs.value);
                default -> apply(op, rhs.value);
            }));
        } catch (ArithmeticException e) {
            return Either.right(
                "integer overflow in expression of type '" + reporter.white("int") + "' results in '" +
                reporter.white(String.valueOf(apply(op, rhs).value)) + "'");
        }
    }

    public ConstantInt apply(Comparison cmp, ConstantInt rhs) {
        return apply(cmp.toBinaryOperator(), rhs);
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
