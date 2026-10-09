package net.flymachine.minecraftclanguage.content.logic.compiler.frontend.c99.helper.constexprHelper;

import net.flymachine.minecraftclanguage.content.logic.compiler.common.constant.PointerConstant;
import net.flymachine.minecraftclanguage.content.logic.compiler.common.type.Field;
import net.flymachine.minecraftclanguage.content.logic.compiler.common.type.Type;

public sealed interface LValuePath {

    record Root(String symbol, Type type) implements LValuePath { }

    record Member(LValuePath base, Field member) implements LValuePath { }

    record Dereference(PointerConstant addr) implements LValuePath { }
}
