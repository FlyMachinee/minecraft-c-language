package net.flymachine.minecraftclanguage.content.logic.compiler.common.staticInit;

import org.jetbrains.annotations.NotNull;

public record UnsignedIntInit(int value) implements StaticInit {

    public static final UnsignedIntInit ZERO = new UnsignedIntInit(0);

    @Override
    public @NotNull String toString() {
        return Integer.toUnsignedString(value) + "U";
    }

    @Override
    public long toByteRepresentation() {
        return Integer.toUnsignedLong(value);
    }
}
