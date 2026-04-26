package net.flymachine.minecraftclanguage.content.logic.compiler.frontend.c99.ast.node;

public class FunctionDefinitionNode implements AstNode {
    public String name;
    public StatementNode body;

    public FunctionDefinitionNode(String name, StatementNode body) {
        this.name = name;
        this.body = body;
    }

    @Override
    public void genFormattedString(StringBuilder stringBuilder, int indentLevel, boolean indentFirstLine) {
        if (indentFirstLine) { stringBuilder.append("  ".repeat(indentLevel)); }
        stringBuilder.append("FunctionDefinitionNode(\n");
        stringBuilder.append("  ".repeat(indentLevel + 1));
        stringBuilder.append("name=\"").append(name).append("\",\n");
        stringBuilder.append("  ".repeat(indentLevel + 1));
        stringBuilder.append("body=");
        body.genFormattedString(stringBuilder, indentLevel + 1, false);
        stringBuilder.append("  ".repeat(indentLevel));
        stringBuilder.append(")\n");
    }
}
