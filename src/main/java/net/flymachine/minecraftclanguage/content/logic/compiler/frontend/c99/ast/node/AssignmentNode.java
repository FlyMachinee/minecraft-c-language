package net.flymachine.minecraftclanguage.content.logic.compiler.frontend.c99.ast.node;

import net.flymachine.minecraftclanguage.content.logic.compiler.common.AssignmentOperator;
import net.flymachine.minecraftclanguage.content.logic.compiler.frontend.c99.ast.AstVisitor;

public class AssignmentNode implements ExpressionNode {
    public AssignmentOperator operator;
    public ExpressionNode lhs;
    public ExpressionNode rhs;

    public AssignmentNode(ExpressionNode lhs, ExpressionNode rhs) {
        operator = AssignmentOperator.ASSIGN;
        this.lhs = lhs;
        this.rhs = rhs;
    }

    public AssignmentNode(AssignmentOperator operator, ExpressionNode lhs, ExpressionNode rhs) {
        this.operator = operator;
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
        stringBuilder.append("AssignmentNode(\n");
        stringBuilder.append("  ".repeat(indentLevel + 1)).append("op=").append(operator).append("\n");
        stringBuilder.append("  ".repeat(indentLevel + 1)).append("lhs=");
        lhs.genFormattedString(stringBuilder, indentLevel + 1, false);
        stringBuilder.append("  ".repeat(indentLevel + 1)).append("rhs=");
        rhs.genFormattedString(stringBuilder, indentLevel + 1, false);
        stringBuilder.append("  ".repeat(indentLevel)).append(")\n");
    }
}
