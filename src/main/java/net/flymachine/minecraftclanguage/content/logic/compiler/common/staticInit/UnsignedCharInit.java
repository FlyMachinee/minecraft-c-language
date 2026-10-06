package net.flymachine.minecraftclanguage.content.logic.compiler.common.staticInit;

import org.jetbrains.annotations.NotNull;

public record UnsignedCharInit(byte value) implements StaticInit {

    public static final UnsignedCharInit ZERO = new UnsignedCharInit((byte) 0);

    @Override
    public @NotNull String toString() {
        return (value & 0xFF) + "UC";
    }

    @Override
    public long toByteRepresentation() {
        return value & 0xFF;
    }
}
