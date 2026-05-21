package net.flymachine.minecraftclanguage.content.logic.compiler.frontend.c99.ast.node;

import net.flymachine.minecraftclanguage.content.logic.compiler.frontend.c99.ast.AstVisitor;
import net.flymachine.minecraftclanguage.content.logic.errorHandle.SourceLocation;

import java.util.List;
import java.io.PrintStream;

public final class ProgramNode extends AstNode {
    public List<ExternalDeclarationNode> extDecls;

    public ProgramNode(SourceLocation wholeLocation, List<ExternalDeclarationNode> extDecls) {
        super(wholeLocation);
        this.extDecls = extDecls;
    }

    @Override
    public <T> T accept(AstVisitor<T> visitor) {
        return visitor.visit(this);
    }

    @Override
    public void dump(PrintStream stream, int indentLevel, boolean indentFirstLine) {
        if (indentFirstLine) { indent(stream, indentLevel);
        }

        stream.println("ProgramNode(extDecls=[");

        for (ExternalDeclarationNode declaration : extDecls) {
            declaration.dump(stream, indentLevel + 1, true);
            stream.println(',');
        }

        indent(stream, indentLevel);
        stream.print("])");
    }
}
