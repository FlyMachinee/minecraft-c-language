package net.flymachine.minecraftclanguage.content.logic.compiler.common.staticInit;

import net.flymachine.minecraftclanguage.content.logic.compiler.common.constant.*;

public record DoubleInit(double value) implements StaticInit {

    public static final DoubleInit ZERO = new DoubleInit(0.0);

    @Override
    public Constant toConstant() {
        return toConstantDouble();
    }

    @Override
    public ConstantInt toConstantInt() {
        return toConstantDouble().toInt();
    }

    @Override
    public ConstantLong toConstantLong() {
        return toConstantDouble().toLong();
    }

    @Override
    public ConstantUnsignedInt toConstantUnsignedInt() {
        return toConstantDouble().toUnsignedInt();
    }

    @Override
    public ConstantUnsignedLong toConstantUnsignedLong() {
        return toConstantDouble().toUnsignedLong();
    }

    @Override
    public ConstantDouble toConstantDouble() {
        return new ConstantDouble(value);
    }
}
