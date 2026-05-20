package net.flymachine.minecraftclanguage.content.logic.compiler.common.staticInit;

import net.flymachine.minecraftclanguage.content.logic.compiler.common.constant.Constant;
import net.flymachine.minecraftclanguage.content.logic.compiler.common.constant.ConstantInt;
import net.flymachine.minecraftclanguage.content.logic.compiler.common.constant.ConstantLong;

public sealed interface StaticInit permits IntInit, LongInit {
    Constant toConstant();

    ConstantInt toConstantInt();

    ConstantLong toConstantLong();
}
