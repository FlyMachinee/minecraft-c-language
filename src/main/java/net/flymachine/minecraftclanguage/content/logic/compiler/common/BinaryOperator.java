package net.flymachine.minecraftclanguage.content.logic.compiler.common;

public enum BinaryOperator {
    /**
     * 加法
     */
    ADD("+"),

    /**
     * 减法
     */
    SUBTRACT("-"),

    /**
     * 乘法
     */
    MULTIPLY("*"),

    /**
     * 除法
     */
    DIVIDE("/"),

    /**
     * 取模
     */
    MODULO("%");

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
