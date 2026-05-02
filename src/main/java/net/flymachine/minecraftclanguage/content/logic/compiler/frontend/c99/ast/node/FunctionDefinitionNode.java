package net.flymachine.minecraftclanguage.content.logic.compiler.frontend.c99.ast.node;

import net.flymachine.minecraftclanguage.content.logic.compiler.frontend.c99.ast.AstVisitor;

import java.util.List;

public class FunctionDefinitionNode implements AstNode {
    public String name;
    public List<BlockItemNode> body;

    public FunctionDefinitionNode(String name, List<BlockItemNode> body) {
        this.name = name;
        this.body = body;
    }

    @Override
    public <T> T accept(AstVisitor<T> visitor) {
        return visitor.visit(this);
    }

    @Override
    public void genFormattedString(StringBuilder stringBuilder, int indentLevel, boolean indentFirstLine) {
        if (indentFirstLine) { stringBuilder.append("  ".repeat(indentLevel)); }
        stringBuilder.append("FunctionDefinitionNode(\n");
        stringBuilder.append("  ".repeat(indentLevel + 1));
        stringBuilder.append("name=\"").append(name).append("\",\n");
        stringBuilder.append("  ".repeat(indentLevel + 1));
        stringBuilder.append("body=[\n");
        for (BlockItemNode item : body) {
            item.genFormattedString(stringBuilder, indentLevel + 2, true);
            stringBuilder.append("  ".repeat(indentLevel + 2)).append(",\n");
        }
        stringBuilder.append("  ".repeat(indentLevel + 1)).append("]\n");
        stringBuilder.append("  ".repeat(indentLevel)).append(")\n");
    }
}
