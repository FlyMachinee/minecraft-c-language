package net.flymachine.minecraftclanguage.content.logic.compiler.frontend.c99.ast.node;

import net.flymachine.minecraftclanguage.content.logic.errorHandle.SourceLocation;

public abstract class BlockItemNode extends AstNode {

    protected BlockItemNode(SourceLocation wholeLocation) {
        super(wholeLocation);
    }
}
