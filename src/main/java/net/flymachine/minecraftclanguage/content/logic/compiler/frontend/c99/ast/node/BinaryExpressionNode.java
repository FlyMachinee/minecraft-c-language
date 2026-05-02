package net.flymachine.minecraftclanguage.content.logic.compiler.frontend.c99.ast.node;

import net.flymachine.minecraftclanguage.content.logic.compiler.common.BinaryOperator;
import net.flymachine.minecraftclanguage.content.logic.compiler.frontend.c99.ast.AstVisitor;

public class BinaryExpressionNode implements ExpressionNode {
    public BinaryOperator op;
    public ExpressionNode lhs;
    public ExpressionNode rhs;

    public BinaryExpressionNode(BinaryOperator op, ExpressionNode lhs, ExpressionNode rhs) {
        this.op = op;
        this.lhs = lhs;
        this.rhs = rhs;
    }

    @Override
    public <T> T accept(AstVisitor<T> visitor) {
        return visitor.visit(this);
    }

    @Override
    public void genFormattedString(StringBuilder stringBuilder, int indentLevel, boolean indentFirstLine) {
        if (indentFirstLine) { stringBuilder.append("  ".repeat(indentLevel)); }
        stringBuilder.append("BinaryExpressionNode(\n");
        stringBuilder.append("  ".repeat(indentLevel + 1));
        stringBuilder.append("op=").append(op).append(",\n");
        stringBuilder.append("  ".repeat(indentLevel + 1));
        stringBuilder.append("lhs=");
        lhs.genFormattedString(stringBuilder, indentLevel + 1, false);
        stringBuilder.append("  ".repeat(indentLevel + 1));
        stringBuilder.append("rhs=");
        rhs.genFormattedString(stringBuilder, indentLevel + 1, false);
        stringBuilder.append("  ".repeat(indentLevel));
        stringBuilder.append(")\n");
    }
}
