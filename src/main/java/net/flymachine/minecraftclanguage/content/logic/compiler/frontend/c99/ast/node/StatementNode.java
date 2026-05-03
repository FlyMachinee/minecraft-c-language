package net.flymachine.minecraftclanguage.content.logic.compiler.frontend.c99.ast.node;

import net.flymachine.minecraftclanguage.content.logic.errorHandle.SourceLocation;

public abstract class StatementNode extends BlockItemNode {

    protected StatementNode(SourceLocation wholeLocation) {
        super(wholeLocation);
    }
}
