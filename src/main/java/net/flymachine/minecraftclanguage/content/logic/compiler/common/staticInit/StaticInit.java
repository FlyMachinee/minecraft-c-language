package net.flymachine.minecraftclanguage.content.logic.compiler.common.staticInit;

import net.flymachine.minecraftclanguage.content.logic.compiler.common.constant.*;

public sealed interface StaticInit permits DoubleInit, IntInit, LongInit, UnsignedIntInit, UnsignedLongInit {
    Constant toConstant();

    ConstantInt toConstantInt();

    ConstantLong toConstantLong();

    ConstantUnsignedInt toConstantUnsignedInt();

    ConstantUnsignedLong toConstantUnsignedLong();

    ConstantDouble toConstantDouble();
}
