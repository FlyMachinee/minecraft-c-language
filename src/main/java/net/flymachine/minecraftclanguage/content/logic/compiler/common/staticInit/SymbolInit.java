package net.flymachine.minecraftclanguage.content.logic.compiler.common.staticInit;

public record SymbolInit(String symbol, long offset) implements StaticInit {
    @Override
    public long toByteRepresentation() {
        throw new UnsupportedOperationException("Cannot determine value in compile time");
    }
}
