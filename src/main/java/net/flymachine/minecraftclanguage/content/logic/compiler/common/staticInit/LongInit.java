package net.flymachine.minecraftclanguage.content.logic.compiler.common.staticInit;

import org.jetbrains.annotations.NotNull;

public record LongInit(long value) implements StaticInit {

    public static final LongInit ZERO = new LongInit(0L);

    @Override
    public @NotNull String toString() {
        return value + "LL";
    }

    @Override
    public long toByteRepresentation() {
        return value;
    }
}
