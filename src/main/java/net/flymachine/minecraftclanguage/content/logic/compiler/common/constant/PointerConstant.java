package net.flymachine.minecraftclanguage.content.logic.compiler.common.constant;

import net.flymachine.minecraftclanguage.content.logic.compiler.common.type.Type;

public sealed interface PointerConstant extends Constant permits ConstantPointer, ConstantSymbolPointer {

    Type referencedType();
}
