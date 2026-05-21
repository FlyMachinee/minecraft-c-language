package net.flymachine.minecraftclanguage.content.logic.compiler.common.staticInit;

import net.flymachine.minecraftclanguage.content.logic.compiler.common.constant.*;
import org.jetbrains.annotations.NotNull;

public record IntInit(int value) implements StaticInit {

    public static final IntInit ZERO = new IntInit(0);

    @Override
    public @NotNull String toString() {
        return Integer.toString(value);
    }

    @Override
    public Constant toConstant() {
        return toConstantInt();
    }

    @Override
    public ConstantInt toConstantInt() {
        return new ConstantInt(value);
    }

    @Override
    public ConstantLong toConstantLong() {
        return new ConstantLong(value);
    }

    @Override
    public ConstantUnsignedInt toConstantUnsignedInt() {
        return new ConstantUnsignedInt(value);
    }

    @Override
    public ConstantUnsignedLong toConstantUnsignedLong() {
        return new ConstantUnsignedLong(value);
    }
}
