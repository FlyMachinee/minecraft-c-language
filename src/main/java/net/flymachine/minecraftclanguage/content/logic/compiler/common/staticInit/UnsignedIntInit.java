package net.flymachine.minecraftclanguage.content.logic.compiler.common.staticInit;

import net.flymachine.minecraftclanguage.content.logic.compiler.common.constant.*;
import org.jetbrains.annotations.NotNull;

public record UnsignedIntInit(int value) implements StaticInit {

    public static final UnsignedIntInit ZERO = new UnsignedIntInit(0);

    @Override
    public @NotNull String toString() {
        return Integer.toUnsignedString(value) + "U";
    }

    @Override
    public Constant toConstant() {
        return toConstantUnsignedInt();
    }

    @Override
    public ConstantInt toConstantInt() {
        return new ConstantInt(value);
    }

    @Override
    public ConstantLong toConstantLong() {
        return new ConstantLong(0xFFFFFFFFL & value);
    }

    @Override
    public ConstantUnsignedInt toConstantUnsignedInt() {
        return new ConstantUnsignedInt(value);
    }

    @Override
    public ConstantUnsignedLong toConstantUnsignedLong() {
        return new ConstantUnsignedLong(0xFFFFFFFFL & value);
    }
}
