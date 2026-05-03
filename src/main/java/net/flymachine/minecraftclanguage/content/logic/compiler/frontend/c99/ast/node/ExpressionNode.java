package net.flymachine.minecraftclanguage.content.logic.compiler.frontend.c99.ast.node;

import net.flymachine.minecraftclanguage.content.logic.errorHandle.SourceLocation;

public abstract class ExpressionNode extends AstNode {
    
    protected ExpressionNode(SourceLocation wholeLocation) {
        super(wholeLocation);
    }
}

