package net.flymachine.minecraftclanguage.content.logic.compiler.frontend.c99.ast.node;

import net.flymachine.minecraftclanguage.content.logic.compiler.frontend.c99.ast.AstVisitor;
import net.flymachine.minecraftclanguage.content.logic.errorHandle.SourceLocation;

public class IncrementDecrementNode extends ExpressionNode {
    public boolean isIncrement;
    public boolean isPrefix;
    public ExpressionNode operand;
    public SourceLocation operatorLocation;

    public IncrementDecrementNode(
        SourceLocation operatorLocation, boolean isIncrement, boolean isPrefix, ExpressionNode operand) {

        super(SourceLocation.concat(operatorLocation, operand.wholeLocation));
        this.operatorLocation = operatorLocation;
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
        stringBuilder.append("  ".repeat(indentLevel)).append(")\n");
    }
}
