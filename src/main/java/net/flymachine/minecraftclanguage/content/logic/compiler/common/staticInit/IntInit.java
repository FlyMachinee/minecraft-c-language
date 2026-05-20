package net.flymachine.minecraftclanguage.content.logic.compiler.common.staticInit;

import net.flymachine.minecraftclanguage.content.logic.compiler.common.constant.Constant;
import net.flymachine.minecraftclanguage.content.logic.compiler.common.constant.ConstantInt;
import net.flymachine.minecraftclanguage.content.logic.compiler.common.constant.ConstantLong;
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
}
