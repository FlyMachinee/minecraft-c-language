package net.flymachine.minecraftclanguage.content.logic.compiler.common.staticInit;

import net.flymachine.minecraftclanguage.content.logic.compiler.common.constant.Constant;
import net.flymachine.minecraftclanguage.content.logic.compiler.common.constant.ConstantInt;
import net.flymachine.minecraftclanguage.content.logic.compiler.common.constant.ConstantLong;
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
        return new ConstantInt((int) value);
    }

    @Override
    public ConstantLong toConstantLong() {
        return new ConstantLong(value);
    }
}
