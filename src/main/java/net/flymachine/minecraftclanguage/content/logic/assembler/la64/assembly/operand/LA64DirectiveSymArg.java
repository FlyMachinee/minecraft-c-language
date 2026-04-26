package net.flymachine.minecraftclanguage.content.logic.assembler.la64.assembly.operand;

import org.jetbrains.annotations.NotNull;

public record LA64DirectiveSymArg(String symbol, long offset) implements LA64DirectiveArgument {

    public LA64DirectiveSymArg(String symbol) {
        this(symbol, 0);
    }

    @Override
    public @NotNull String toString() {
        return symbol;
    }
}
