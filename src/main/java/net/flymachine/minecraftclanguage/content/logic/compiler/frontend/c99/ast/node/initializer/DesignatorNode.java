package net.flymachine.minecraftclanguage.content.logic.compiler.frontend.c99.ast.node.initializer;

import net.flymachine.minecraftclanguage.content.logic.compiler.frontend.c99.ast.node.AstInterface;

public sealed interface DesignatorNode extends AstInterface permits ArrayDesignatorNode, MemberDesignatorNode {
}
