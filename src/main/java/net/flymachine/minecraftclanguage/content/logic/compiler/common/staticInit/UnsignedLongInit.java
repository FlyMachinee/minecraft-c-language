package net.flymachine.minecraftclanguage.content.logic.compiler.common.staticInit;

import net.flymachine.minecraftclanguage.content.logic.compiler.common.constant.*;
import org.jetbrains.annotations.NotNull;

public record UnsignedLongInit(long value) implements StaticInit {

    public static final UnsignedLongInit ZERO = new UnsignedLongInit(0L);

    @Override
    public @NotNull String toString() {
        return Long.toUnsignedString(value) + "ULL";
    }

    @Override
    public Constant toConstant() {
        return toConstantUnsignedLong();
    }

    @Override
    public ConstantInt toConstantInt() {
        return new ConstantInt((int) value);
    }

    @Override
    public ConstantLong toConstantLong() {
        return new ConstantLong(value);
    }

    @Override
    public ConstantUnsignedInt toConstantUnsignedInt() {
        return new ConstantUnsignedInt((int) value);
    }

    @Override
    public ConstantUnsignedLong toConstantUnsignedLong() {
        return new ConstantUnsignedLong(value);
    }
}
