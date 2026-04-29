package net.flymachine.minecraftclanguage.content.logic.compiler.frontend.c99.ast.node;

import net.flymachine.minecraftclanguage.content.logic.compiler.common.UnaryOperator;

public class UnaryExpressionNode implements ExpressionNode {
    public UnaryOperator op;
    public ExpressionNode exp;

    public UnaryExpressionNode(UnaryOperator op, ExpressionNode exp) {
        this.op = op;
        this.exp = exp;
    }

    @Override
    public void genFormattedString(StringBuilder stringBuilder, int indentLevel, boolean indentFirstLine) {
        if (indentFirstLine) { stringBuilder.append("  ".repeat(indentLevel)); }
        stringBuilder.append("UnaryExpressionNode(\n");
        stringBuilder.append("  ".repeat(indentLevel + 1));
        stringBuilder.append("op=").append(op).append(",\n");
        stringBuilder.append("  ".repeat(indentLevel + 1));
        stringBuilder.append("exp=");
        exp.genFormattedString(stringBuilder, indentLevel + 1, false);
        stringBuilder.append("  ".repeat(indentLevel));
        stringBuilder.append(")\n");
    }
}
