package net.flymachine.minecraftclanguage.content.logic.compiler.common;

public enum Comparison {
    EQUAL, NOT_EQUAL, LESS, LESS_EQUAL, GREATER, GREATER_EQUAL;

    public BinaryOperator toBinaryOperator() {
        return switch (this) {
            case EQUAL -> BinaryOperator.EQUAL;
            case NOT_EQUAL -> BinaryOperator.NOT_EQUAL;
            case LESS -> BinaryOperator.LESS_THAN;
            case GREATER -> BinaryOperator.GREATER_THAN;
            case LESS_EQUAL -> BinaryOperator.LESS_OR_EQUAL;
            case GREATER_EQUAL -> BinaryOperator.GREATER_OR_EQUAL;
        };
    }
}
