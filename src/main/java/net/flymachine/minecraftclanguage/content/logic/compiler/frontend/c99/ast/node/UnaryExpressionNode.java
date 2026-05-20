package net.flymachine.minecraftclanguage.content.logic.compiler.frontend.c99.ast.node;

import net.flymachine.minecraftclanguage.content.logic.compiler.frontend.c99.ast.AstVisitor;
import net.flymachine.minecraftclanguage.content.logic.compiler.frontend.c99.ast.ExpressionBoolVisitor;
import net.flymachine.minecraftclanguage.content.logic.compiler.frontend.c99.ast.ExpressionVisitor;
import net.flymachine.minecraftclanguage.content.logic.compiler.ir.TacValue;
import net.flymachine.minecraftclanguage.content.logic.errorHandle.SourceLocation;

public final class UnaryExpressionNode extends ExpressionNode {
    public UnaryOperatorNode op;
    public ExpressionNode exp;

    public UnaryExpressionNode(UnaryOperatorNode op, ExpressionNode exp) {
        super(SourceLocation.concat(op.wholeLoc, exp.wholeLoc));
        this.op = op;
        this.exp = exp;
    }

    @Override
    public <T> T accept(AstVisitor<T> visitor) {
        return visitor.visit(this);
    }

    @Override
    public void genFormattedString(StringBuilder stringBuilder, int indentLevel, boolean indentFirstLine) {
        if (indentFirstLine) { stringBuilder.append("  ".repeat(indentLevel)); }
        stringBuilder.append("UnaryExpressionNode(\n").append("  ".repeat(indentLevel + 1)).append("op=");
        op.genFormattedString(stringBuilder);
        stringBuilder.append(",\n").append("  ".repeat(indentLevel + 1)).append("exp=");
        exp.genFormattedString(stringBuilder, indentLevel + 1, false);
        if (expType != null) {
            stringBuilder.append("  ".repeat(indentLevel + 1)).append("expType=").append(expType).append(",\n");
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
