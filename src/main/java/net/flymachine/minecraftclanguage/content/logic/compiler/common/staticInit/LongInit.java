package net.flymachine.minecraftclanguage.content.logic.compiler.common.staticInit;

import net.flymachine.minecraftclanguage.content.logic.compiler.common.constant.*;
import org.jetbrains.annotations.NotNull;

public record LongInit(long value) implements StaticInit {

    public static final LongInit ZERO = new LongInit(0L);

    @Override
    public @NotNull String toString() {
        return value + "LL";
    }

    @Override
    public Constant toConstant() {
        return toConstantLong();
    }

    @Override
    public ConstantInt toConstantInt() {
        return toConstantLong().toInt();
    }

    @Override
    public ConstantLong toConstantLong() {
        return new ConstantLong(value);
    }

    @Override
    public ConstantUnsignedInt toConstantUnsignedInt() {
        return toConstantLong().toUnsignedInt();
    }

    @Override
    public ConstantUnsignedLong toConstantUnsignedLong() {
        return toConstantLong().toUnsignedLong();
    }

    @Override
    public ConstantDouble toConstantDouble() {
        return toConstantLong().toDouble();
    }
}
