package net.flymachine.minecraftclanguage.content.logic.compiler.frontend.c99.ast.node;

import net.flymachine.minecraftclanguage.content.logic.compiler.frontend.c99.ast.AstVisitor;
import net.flymachine.minecraftclanguage.content.logic.errorHandle.SourceLocation;

import java.util.List;

public final class ProgramNode extends AstNode {
    public List<ExternalDeclarationNode> declarations;

    public ProgramNode(SourceLocation wholeLocation, List<ExternalDeclarationNode> declarations) {
        super(wholeLocation);
        this.declarations = declarations;
    }

    @Override
    public <T> T accept(AstVisitor<T> visitor) {
        return visitor.visit(this);
    }

    @Override
    public void genFormattedString(StringBuilder stringBuilder, int indentLevel, boolean indentFirstLine) {
        if (indentFirstLine) { stringBuilder.append("  ".repeat(indentLevel)); }
        stringBuilder.append("ProgramNode(externalDecls=[\n");
        for (ExternalDeclarationNode declaration : declarations) {
            declaration.genFormattedString(stringBuilder, indentLevel + 1, true);
            stringBuilder.append("  ".repeat(indentLevel + 1)).append(",\n");
        }
        stringBuilder.append("])\n");
    }
}
