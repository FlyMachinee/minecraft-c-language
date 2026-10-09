package net.flymachine.minecraftclanguage.content.logic.compiler.frontend.c99.ast.node;

import net.flymachine.minecraftclanguage.content.logic.compiler.frontend.c99.ast.AstVisitor;
import net.flymachine.minecraftclanguage.content.logic.errorHandle.SourceLocation;

import java.io.PrintStream;

public final class MemberDeclaratorNode extends AstNode {
    public final TypeNode finalType;
    public IdentifierNode id;

    public MemberDeclaratorNode(SourceLocation wholeLocation, TypeNode finalType, IdentifierNode id) {
        super(wholeLocation);
        this.finalType = finalType;
        this.id = id;
    }

    @Override
    public <T> T accept(AstVisitor<T> visitor) {
        return null;
    }

    @Override
    public void dump(PrintStream stream, int indentLevel, boolean indentFirstLine) { }
}
