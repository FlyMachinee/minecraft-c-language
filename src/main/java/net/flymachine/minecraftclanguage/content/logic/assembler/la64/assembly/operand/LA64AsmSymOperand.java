package net.flymachine.minecraftclanguage.content.logic.assembler.la64.assembly.operand;

import org.jetbrains.annotations.NotNull;

public record LA64AsmSymOperand(String symbol, int offset) implements LA64AsmOperand {

    public LA64AsmSymOperand(String symbol) {
        this(symbol, 0);
    }

    @Override
    public @NotNull String toString() {
        return symbol;
    }
}
