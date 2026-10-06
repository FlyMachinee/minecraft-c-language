package net.flymachine.minecraftclanguage.content.logic.compiler.common.staticInit;

import org.jetbrains.annotations.NotNull;

public record CharInit(byte value) implements StaticInit {

    public static final CharInit ZERO = new CharInit((byte) 0);

    @Override
    public @NotNull String toString() {
        return value + "C";
    }

    @Override
    public long toByteRepresentation() {
        return value;
    }
}
