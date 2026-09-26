package net.flymachine.minecraftclanguage.content.logic.compiler.common.staticInit;

import org.jetbrains.annotations.NotNull;

public record DoubleInit(double value) implements StaticInit {

    public static final DoubleInit ZERO = new DoubleInit(0.0);

    @Override
    public @NotNull String toString() {
        return Double.toString(value);
    }

    @Override
    public long toByteRepresentation() {
        return Double.doubleToLongBits(value);
    }
}
