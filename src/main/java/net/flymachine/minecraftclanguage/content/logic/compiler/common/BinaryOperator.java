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
    MODULO("%"),

    /**
     * 左移
     */
    LEFT_SHIFT("<<"),

    /**
     * 右移（实现为算术右移）
     */
    RIGHT_SHIFT(">>"),

    /**
     * 按位与
     */
    BITWISE_AND("&"),

    /**
     * 按位或
     */
    BITWISE_OR("|"),

    /**
     * 按位异或
     */
    BITWISE_XOR("^");

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
