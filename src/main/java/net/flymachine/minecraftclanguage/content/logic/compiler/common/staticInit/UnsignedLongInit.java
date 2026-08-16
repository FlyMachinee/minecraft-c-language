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
        return toConstantUnsignedLong().toInt();
    }

    @Override
    public ConstantLong toConstantLong() {
        return toConstantUnsignedLong().toLong();
    }

    @Override
    public ConstantUnsignedInt toConstantUnsignedInt() {
        return toConstantUnsignedLong().toUnsignedInt();
    }

    @Override
    public ConstantUnsignedLong toConstantUnsignedLong() {
        return new ConstantUnsignedLong(value);
    }

    @Override
    public ConstantDouble toConstantDouble() {
        return toConstantUnsignedLong().toDouble();
    }
}
