package net.flymachine.minecraftclanguage.content.logic.compiler.common.util;

import net.flymachine.minecraftclanguage.content.logic.compiler.common.Comparison;
import net.flymachine.minecraftclanguage.content.logic.compiler.common.constant.ConstantLong;
import net.flymachine.minecraftclanguage.content.logic.compiler.common.constant.IntegerConstant;
import net.flymachine.minecraftclanguage.content.logic.compiler.frontend.c99.ast.node.ConstantNode;
import net.flymachine.minecraftclanguage.content.logic.compiler.frontend.c99.ast.node.ExpressionNode;

import java.util.Optional;

public class UndefinedBehaviourUtil {

    public static Optional<String> checkBitwiseShift(boolean isLeftShift, ExpressionNode lhs, ExpressionNode rhs) {
        if (isLeftShift && lhs instanceof ConstantNode constLhs && constLhs.value instanceof IntegerConstant intLhs) {
            if (intLhs.isNegative()) {
                return Optional.of("left shift of negative value");
            }
        }
        if (rhs instanceof ConstantNode constRhs && constRhs.value instanceof IntegerConstant intRhs) {
            long width = intRhs.getType().sizeof() * 8;
            if (intRhs.isNegative()) {
                return Optional.of((isLeftShift ? "left" : "right") + " shift count is negative");
            }
            if (intRhs.apply(Comparison.GREATER_EQUAL, new ConstantLong(width).castTo(intRhs.getType())).isPositive()) {
                return Optional.of((isLeftShift ? "left" : "right") + " shift count >= width of type");
            }
        }
        return Optional.empty();
    }

    public static Optional<String> checkDivision(ExpressionNode rhs) {
        if (rhs instanceof ConstantNode constRhs && constRhs.value instanceof IntegerConstant intRhs) {
            if (intRhs.isZero()) {
                return Optional.of("division by zero");
            }
        }
        return Optional.empty();
    }
}
