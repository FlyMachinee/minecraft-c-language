package net.flymachine.minecraftclanguage.content.logic.compiler.frontend.c99.ast.node;

import net.flymachine.minecraftclanguage.content.logic.compiler.frontend.c99.ast.AstVisitor;
import net.flymachine.minecraftclanguage.content.logic.compiler.frontend.c99.ast.ExpressionBoolVisitor;
import net.flymachine.minecraftclanguage.content.logic.compiler.frontend.c99.ast.ExpressionVisitor;
import net.flymachine.minecraftclanguage.content.logic.compiler.ir.TacValue;
import net.flymachine.minecraftclanguage.content.logic.errorHandle.SourceLocation;

public final class ConditionalExpressionNode extends ExpressionNode {
    public ExpressionNode cond;
    public ExpressionNode thenExp;
    public ExpressionNode elseExp;

    public ConditionalExpressionNode(ExpressionNode cond, ExpressionNode thenExp, ExpressionNode elseExp) {
        super(SourceLocation.concat(cond.wholeLoc, elseExp.wholeLoc));
        this.cond = cond;
        this.thenExp = thenExp;
        this.elseExp = elseExp;
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
        thenExp.genFormattedString(stringBuilder, indentLevel + 1, false);
        stringBuilder.append("  ".repeat(indentLevel + 1)).append("elseExpr=");
        elseExp.genFormattedString(stringBuilder, indentLevel + 1, false);
        if (expType != null) {
            stringBuilder.append("  ".repeat(indentLevel + 1)).append("expType=").append(expType).append("\n");
        }
        stringBuilder.append("  ".repeat(indentLevel)).append(")\n");
    }

    @Override
    public TacValue accept(ExpressionVisitor visitor) {
        return visitor.visit(this);
    }

    @Override
    public ExpressionBoolVisitor.BoolGenResult accept(
        ExpressionBoolVisitor visitor, String jumpTarget, boolean inverse) {
        return visitor.visit(this, jumpTarget, inverse);
    }
}
