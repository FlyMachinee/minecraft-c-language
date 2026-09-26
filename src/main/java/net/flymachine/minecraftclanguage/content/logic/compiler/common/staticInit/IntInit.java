package net.flymachine.minecraftclanguage.content.logic.compiler.common.staticInit;

import org.jetbrains.annotations.NotNull;

public record IntInit(int value) implements StaticInit {

    public static final IntInit ZERO = new IntInit(0);

    @Override
    public @NotNull String toString() {
        return Integer.toString(value);
    }

    @Override
    public long toByteRepresentation() {
        return value;
    }
}
