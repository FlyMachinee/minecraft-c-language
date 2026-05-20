package net.flymachine.minecraftclanguage.content.logic.compiler.frontend.c99.ast.node;

import net.flymachine.minecraftclanguage.content.logic.compiler.frontend.c99.ast.AstVisitor;
import net.flymachine.minecraftclanguage.content.logic.compiler.frontend.c99.ast.ExpressionBoolVisitor;
import net.flymachine.minecraftclanguage.content.logic.compiler.frontend.c99.ast.ExpressionVisitor;
import net.flymachine.minecraftclanguage.content.logic.compiler.ir.TacValue;
import net.flymachine.minecraftclanguage.content.logic.errorHandle.SourceLocation;

public final class IncrementDecrementNode extends ExpressionNode {
    public boolean isIncrement;
    public boolean isPrefix;
    public ExpressionNode operand;
    public SourceLocation operatorLoc;

    public IncrementDecrementNode(
        SourceLocation operatorLoc, boolean isIncrement, boolean isPrefix, ExpressionNode operand) {

        super(SourceLocation.concat(operatorLoc, operand.wholeLoc));
        this.operatorLoc = operatorLoc;
        this.isIncrement = isIncrement;
        this.isPrefix = isPrefix;
        this.operand = operand;
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
        stringBuilder.append(isIncrement ? "IncrementNode(\n" : "DecrementNode(\n");
        stringBuilder.append("  ".repeat(indentLevel + 1)).append("prefix=").append(isPrefix).append("\n");
        stringBuilder.append("  ".repeat(indentLevel + 1)).append("operand=");
        operand.genFormattedString(stringBuilder, indentLevel + 1, false);
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
