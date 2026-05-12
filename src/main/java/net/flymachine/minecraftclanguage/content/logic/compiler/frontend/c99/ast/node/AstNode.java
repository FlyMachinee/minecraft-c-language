package net.flymachine.minecraftclanguage.content.logic.compiler.frontend.c99.ast.node;

import net.flymachine.minecraftclanguage.content.logic.errorHandle.SourceLocation;

public abstract class AstNode implements AstInterface {
    public final SourceLocation wholeLocation;

    @Override
    public SourceLocation getWholeLocation() {
        return wholeLocation;
    }

    protected AstNode(SourceLocation wholeLocation) {
        this.wholeLocation = wholeLocation;
    }
}
