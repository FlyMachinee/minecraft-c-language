package net.flymachine.minecraftclanguage.content.logic.compiler.frontend.c99.ast.node;

import net.flymachine.minecraftclanguage.content.logic.compiler.frontend.c99.ast.AstVisitor;

public class ProgramNode implements AstNode {
    public FunctionDefinitionNode functionDefinition;

    public ProgramNode(FunctionDefinitionNode functionDefinition) {
        this.functionDefinition = functionDefinition;
    }

    @Override
    public <T> T accept(AstVisitor<T> visitor) {
        return visitor.visit(this);
    }

    @Override
    public void genFormattedString(StringBuilder stringBuilder, int indentLevel, boolean indentFirstLine) {
        if (indentFirstLine) { stringBuilder.append("  ".repeat(indentLevel)); }
        stringBuilder.append("ProgramNode(\n");
        functionDefinition.genFormattedString(stringBuilder, indentLevel + 1, true);
        stringBuilder.append("  ".repeat(indentLevel));
        stringBuilder.append(")\n");
    }
}
