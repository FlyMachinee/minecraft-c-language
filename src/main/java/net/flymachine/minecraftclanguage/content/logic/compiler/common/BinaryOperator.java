package net.flymachine.minecraftclanguage.content.logic.compiler.common;

public enum BinaryOperator {
    ADD("+"),
    SUBTRACT("-"),
    MULTIPLY("*"),
    DIVIDE("/"),
    MODULO("%"),
    LEFT_SHIFT("<<"),
    RIGHT_SHIFT(">>"),
    BITWISE_AND("&"),
    BITWISE_OR("|"),
    BITWISE_XOR("^"),
    LOGICAL_AND("&&"),
    LOGICAL_OR("||"),
    LESS_THAN("<"),
    GREATER_THAN(">"),
    EQUAL("=="),
    NOT_EQUAL("!="),
    LESS_OR_EQUAL("<="),
    GREATER_OR_EQUAL(">=");

    private final String symbol;

    BinaryOperator(String symbol) {
        this.symbol = symbol;
    }

    public String getSymbol() {
        return symbol;
    }

    public static BinaryOperator fromSymbol(String symbol) {
        for (BinaryOperator binaryOperator : BinaryOperator.values()) {
            if (binaryOperator.getSymbol().equals(symbol)) {
                return binaryOperator;
            }
        }
        return null;
    }
}
