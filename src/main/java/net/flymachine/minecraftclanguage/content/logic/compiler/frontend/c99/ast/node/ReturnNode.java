package net.flymachine.minecraftclanguage.content.logic.compiler.frontend.c99.ast.node;

// public record ReturnNode(ExpressionNode expression) implements StatementNode {
public class ReturnNode implements StatementNode {
    public ExpressionNode expression;

    public ReturnNode(ExpressionNode expression) {
        this.expression = expression;
    }

    @Override
    public void genFormattedString(StringBuilder stringBuilder, int indentLevel, boolean indentFirstLine) {
        if (indentFirstLine) { stringBuilder.append("  ".repeat(indentLevel)); }
        stringBuilder.append("ReturnNode(\n");
        expression.genFormattedString(stringBuilder, indentLevel + 1, true);
        stringBuilder.append("  ".repeat(indentLevel));
        stringBuilder.append(")\n");
    }
}

