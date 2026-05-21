package net.flymachine.minecraftclanguage.content.logic.compiler.frontend.c99.ast.node;

import net.flymachine.minecraftclanguage.content.logic.errorHandle.SourceLocation;

import java.io.PrintStream;

public abstract class AstNode implements AstInterface {
    public final SourceLocation wholeLoc;

    @Override
    public SourceLocation getWholeLocation() {
        return wholeLoc;
    }

    protected AstNode(SourceLocation wholeLoc) {
        this.wholeLoc = wholeLoc;
    }

    protected void indent(PrintStream stream, int indentLevel) {
        for (int i = 0; i < indentLevel; i++) {
            stream.print("  ");
        }
    }
}
