package net.flymachine.minecraftclanguage.content.logic.compiler.common;

public enum UnaryOperator {
    /**
     * 取相反数
     */
    NEGATE("-"),

    /**
     * 按位取反
     */
    COMPLEMENT("~");

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
