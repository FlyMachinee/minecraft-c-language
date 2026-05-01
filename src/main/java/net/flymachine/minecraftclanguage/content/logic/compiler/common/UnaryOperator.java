package net.flymachine.minecraftclanguage.content.logic.compiler.common;

public enum UnaryOperator {
    NEGATE("-"),
    COMPLEMENT("~"),
    NOT("!");

    private final String symbol;

    UnaryOperator(String symbol) {
        this.symbol = symbol;
    }

    public String getSymbol() {
        return symbol;
    }

    public static UnaryOperator fromSymbol(String symbol) {
        for (UnaryOperator operator : UnaryOperator.values()) {
            if (operator.getSymbol().equals(symbol)) { return operator; }
        }
        return null;
    }
}
