package net.flymachine.minecraftclanguage.content.logic.compiler.frontend.c99.ast.node;

import net.flymachine.minecraftclanguage.content.logic.compiler.frontend.c99.ast.AstVisitor;
import net.flymachine.minecraftclanguage.content.logic.errorHandle.SourceLocation;

import java.io.PrintStream;

public final class IdentifierNode extends AstNode {
    public String id;

    public IdentifierNode(SourceLocation wholeLocation, String id) {
        super(wholeLocation);
        this.id = id;
    }

    @Override
    public <T> T accept(AstVisitor<T> visitor) {
        return null;
    }

    @Override
    public void dump(PrintStream stream, int indentLevel, boolean indentFirstLine) {
        stream.print(id);
    }
}
