package net.flymachine.minecraftclanguage.content.logic.compiler.common.constant;

public sealed interface IntegerConstant extends Constant permits ConstantChar, ConstantInt, ConstantLong, ConstantUnsignedChar, ConstantUnsignedInt, ConstantUnsignedLong {

    boolean isSigned();

    default boolean isUnsigned() {
        return !isSigned();
    }

    boolean isPositive();

    default boolean isNegative() {
        return !isZero() && !isPositive();
    }

    default boolean isNonNegative() {
        return isZero() || isPositive();
    }
}
