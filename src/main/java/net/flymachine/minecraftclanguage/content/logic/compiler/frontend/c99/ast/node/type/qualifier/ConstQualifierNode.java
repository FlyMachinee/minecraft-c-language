package net.flymachine.minecraftclanguage.content.logic.compiler.frontend.c99.ast.node.type.qualifier;

import net.flymachine.minecraftclanguage.content.logic.compiler.frontend.c99.ast.AstVisitor;
import net.flymachine.minecraftclanguage.content.logic.compiler.frontend.c99.ast.node.AstNode;
import net.flymachine.minecraftclanguage.content.logic.errorHandle.SourceLocation;

import java.io.PrintStream;

public final class ConstQualifierNode extends AstNode {

    public ConstQualifierNode(SourceLocation wholeLoc) {
        super(wholeLoc);
    }

    @Override
    public <T> T accept(AstVisitor<T> visitor) {
        return null;
    }

    @Override
    public void dump(PrintStream stream, int indentLevel, boolean indentFirstLine) { }
}
