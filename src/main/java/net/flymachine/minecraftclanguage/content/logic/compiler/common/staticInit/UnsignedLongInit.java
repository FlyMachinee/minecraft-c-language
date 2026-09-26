package net.flymachine.minecraftclanguage.content.logic.compiler.common.staticInit;

import org.jetbrains.annotations.NotNull;

public record UnsignedLongInit(long value) implements StaticInit {

    public static final UnsignedLongInit ZERO = new UnsignedLongInit(0L);

    @Override
    public @NotNull String toString() {
        return Long.toUnsignedString(value) + "ULL";
    }

    @Override
    public long toByteRepresentation() {
        return value;
    }
}
