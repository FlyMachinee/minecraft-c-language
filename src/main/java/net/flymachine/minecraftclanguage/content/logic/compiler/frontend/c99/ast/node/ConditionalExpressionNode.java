package net.flymachine.minecraftclanguage.content.logic.compiler.frontend.c99.ast.node;

import net.flymachine.minecraftclanguage.content.logic.compiler.frontend.c99.ast.AstVisitor;
import net.flymachine.minecraftclanguage.content.logic.errorHandle.SourceLocation;

public final class ConditionalExpressionNode extends ExpressionNode {
    public ExpressionNode cond;
    public ExpressionNode thenExpr;
    public ExpressionNode elseExpr;

    public ConditionalExpressionNode(ExpressionNode cond, ExpressionNode thenExpr, ExpressionNode elseExpr) {
        super(SourceLocation.concat(cond.wholeLocation, elseExpr.wholeLocation));
        this.cond = cond;
        this.thenExpr = thenExpr;
        this.elseExpr = elseExpr;
    }

    @Override
    public <T> T accept(AstVisitor<T> visitor) {
        return visitor.visit(this);
    }

    @Override
    public void genFormattedString(StringBuilder stringBuilder, int indentLevel, boolean indentFirstLine) {
        if (indentFirstLine) {
            stringBuilder.append("    ".repeat(indentLevel));
        }
        stringBuilder.append("ConditionalExpressionNode(\n");
        stringBuilder.append("  ".repeat(indentLevel + 1)).append("cond=");
        cond.genFormattedString(stringBuilder, indentLevel + 1, false);
        stringBuilder.append("  ".repeat(indentLevel + 1)).append("thenExpr=");
        thenExpr.genFormattedString(stringBuilder, indentLevel + 1, false);
        stringBuilder.append("  ".repeat(indentLevel + 1)).append("elseExpr=");
        elseExpr.genFormattedString(stringBuilder, indentLevel + 1, false);
        stringBuilder.append("  ".repeat(indentLevel)).append(")\n");
    }
}
