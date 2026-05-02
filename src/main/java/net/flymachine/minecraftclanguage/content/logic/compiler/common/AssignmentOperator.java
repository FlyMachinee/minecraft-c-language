package net.flymachine.minecraftclanguage.content.logic.compiler.common;

public enum AssignmentOperator {
    ASSIGN("=", null),
    MULTIPLY_ASSIGN("*=", BinaryOperator.MULTIPLY),
    DIVIDE_ASSIGN("/=", BinaryOperator.DIVIDE),
    MODULO_ASSIGN("%=", BinaryOperator.MODULO),
    ADD_ASSIGN("+=", BinaryOperator.ADD),
    SUBTRACT_ASSIGN("-=", BinaryOperator.SUBTRACT),
    LEFT_SHIFT_ASSIGN("<<=", BinaryOperator.LEFT_SHIFT),
    RIGHT_SHIFT_ASSIGN(">>=", BinaryOperator.RIGHT_SHIFT),
    BITWISE_AND_ASSIGN("&=", BinaryOperator.BITWISE_AND),
    BITWISE_OR_ASSIGN("|=", BinaryOperator.BITWISE_OR),
    BITWISE_XOR_ASSIGN("^=", BinaryOperator.BITWISE_XOR);

    private final String symbol;
    private final BinaryOperator binaryOperator;

    AssignmentOperator(final String symbol, final BinaryOperator binaryOperator) {
        this.symbol = symbol;
        this.binaryOperator = binaryOperator;
    }

    public String getSymbol() {
        return symbol;
    }

    public BinaryOperator getBinaryOperator() {
        return binaryOperator;
    }

    public static AssignmentOperator fromSymbol(String symbol) {
        for (final AssignmentOperator assignmentOperator : AssignmentOperator.values()) {
            if (assignmentOperator.getSymbol().equals(symbol)) {
                return assignmentOperator;
            }
        }
        return null;
    }
}
