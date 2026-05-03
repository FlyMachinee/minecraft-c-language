package net.flymachine.minecraftclanguage.content.logic.compiler.frontend.c99.ast.node;

import net.flymachine.minecraftclanguage.content.logic.compiler.frontend.c99.ast.AstVisitor;
import net.flymachine.minecraftclanguage.content.logic.errorHandle.SourceLocation;

public abstract class AstNode {
    public final SourceLocation wholeLocation;

    protected AstNode(SourceLocation wholeLocation) {
        this.wholeLocation = wholeLocation;
    }

    abstract public <T> T accept(AstVisitor<T> visitor);

    abstract public void genFormattedString(StringBuilder stringBuilder, int indentLevel, boolean indentFirstLine);

    public void genFormattedString(StringBuilder stringBuilder) {
        genFormattedString(stringBuilder, 0, false);
    }
}
