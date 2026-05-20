package net.flymachine.minecraftclanguage.content.logic.compiler.frontend.c99.ast.node;

import net.flymachine.minecraftclanguage.content.logic.compiler.frontend.c99.ast.AstVisitor;
import net.flymachine.minecraftclanguage.content.logic.compiler.frontend.c99.ast.ExpressionBoolVisitor;
import net.flymachine.minecraftclanguage.content.logic.compiler.frontend.c99.ast.ExpressionVisitor;
import net.flymachine.minecraftclanguage.content.logic.compiler.ir.TacValue;
import net.flymachine.minecraftclanguage.content.logic.errorHandle.SourceLocation;

public final class CastExpressionNode extends ExpressionNode {
    public TypeNode targetType;
    public ExpressionNode exp;

    public CastExpressionNode(SourceLocation wholeLocation, TypeNode targetType, ExpressionNode exp) {
        super(wholeLocation);
        this.targetType = targetType;
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
        stringBuilder.append("CastNode(\n");
        stringBuilder.append("  ".repeat(indentLevel + 1)).append("targetType=");
        targetType.genFormattedString(stringBuilder);
        stringBuilder.append(",\n");
        stringBuilder.append("  ".repeat(indentLevel + 1)).append("exp=");
        exp.genFormattedString(stringBuilder, indentLevel + 1, false);
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
