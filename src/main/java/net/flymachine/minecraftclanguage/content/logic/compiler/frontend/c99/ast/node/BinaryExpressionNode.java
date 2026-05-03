package net.flymachine.minecraftclanguage.content.logic.compiler.frontend.c99.ast.node;

import net.flymachine.minecraftclanguage.content.logic.compiler.frontend.c99.ast.AstVisitor;
import net.flymachine.minecraftclanguage.content.logic.errorHandle.SourceLocation;

public class BinaryExpressionNode extends ExpressionNode {
    public BinaryOperatorNode op;
    public ExpressionNode lhs;
    public ExpressionNode rhs;

    public BinaryExpressionNode(BinaryOperatorNode op, ExpressionNode lhs, ExpressionNode rhs) {
        super(SourceLocation.concat(lhs.wholeLocation, rhs.wholeLocation));
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
        stringBuilder.append("BinaryExpressionNode(\n").append("  ".repeat(indentLevel + 1)).append("op=");
        op.genFormattedString(stringBuilder);
        stringBuilder.append(",\n").append("  ".repeat(indentLevel + 1)).append("lhs=");
        lhs.genFormattedString(stringBuilder, indentLevel + 1, false);
        stringBuilder.append("  ".repeat(indentLevel + 1)).append("rhs=");
        rhs.genFormattedString(stringBuilder, indentLevel + 1, false);
        stringBuilder.append("  ".repeat(indentLevel)).append(")\n");
    }
}
