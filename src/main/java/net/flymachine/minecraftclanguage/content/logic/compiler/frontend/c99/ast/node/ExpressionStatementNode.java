package net.flymachine.minecraftclanguage.content.logic.compiler.frontend.c99.ast.node;

import net.flymachine.minecraftclanguage.content.logic.compiler.frontend.c99.ast.AstVisitor;

public class ExpressionStatementNode extends StatementNode {
    public ExpressionNode exp;

    public ExpressionStatementNode(ExpressionNode exp) {
        super(exp.wholeLoc);
        this.exp = exp;
    }

    @Override
    public <T> T accept(AstVisitor<T> visitor) {
        return visitor.visit(this);
    }

    @Override
    public void genFormattedString(StringBuilder stringBuilder, int indentLevel, boolean indentFirstLine) {
        if (indentFirstLine) {
            stringBuilder.append("  ".repeat(indentLevel));
        }
        stringBuilder.append("ExpressionStatementNode(\n");
        if (isLabeled()) {
            stringBuilder.append("  ".repeat(indentLevel + 1));
            genFormatedStringForLabels(stringBuilder);
            stringBuilder.append(",\n");
        }
        stringBuilder.append("  ".repeat(indentLevel + 1)).append("exp=");
        exp.genFormattedString(stringBuilder, indentLevel + 1, false);
        stringBuilder.append("  ".repeat(indentLevel)).append(")\n");
    }
}
