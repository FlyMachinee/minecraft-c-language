package net.flymachine.minecraftclanguage.content.logic.compiler.frontend.c99.ast.node;

import net.flymachine.minecraftclanguage.content.logic.compiler.frontend.c99.ast.AstVisitor;
import net.flymachine.minecraftclanguage.content.logic.errorHandle.SourceLocation;

public final class WhileLoopNode extends StatementNode {
    public ExpressionNode cond;
    public StatementNode body;
    public boolean isDoWhile;
    public String loopLabel;

    public WhileLoopNode(SourceLocation wholeLocation, ExpressionNode cond, StatementNode body) {
        super(wholeLocation);
        this.cond = cond;
        this.body = body;
        this.isDoWhile = false;
    }

    public WhileLoopNode(SourceLocation wholeLocation, ExpressionNode cond, StatementNode body, boolean isDoWhile) {
        super(wholeLocation);
        this.cond = cond;
        this.body = body;
        this.isDoWhile = true;
    }

    @Override
    public boolean containsActiveGotoLabel() {
        if (super.containsActiveGotoLabel()) {
            return true;
        }
        return body.containsActiveGotoLabel();
    }

    @Override
    public <T> T accept(AstVisitor<T> visitor) {
        return visitor.visit(this);
    }

    @Override
    public void genFormattedString(StringBuilder stringBuilder, int indentLevel, boolean indentFirstLine) {
        if (indentFirstLine) { stringBuilder.append("  ".repeat(indentLevel)); }
        stringBuilder.append(isDoWhile ? "DoWhileLoopNode(\n" : "WhileLoopNode(\n");
        if (loopLabel != null) {
            stringBuilder.append("  ".repeat(indentLevel + 1)).append("loop=").append(loopLabel).append(",\n");
        }
        if (isDoWhile) {
            stringBuilder.append("  ".repeat(indentLevel + 1)).append("body=");
            body.genFormattedString(stringBuilder, indentLevel + 1, false);
            stringBuilder.append("  ".repeat(indentLevel + 1)).append("cond=");
            cond.genFormattedString(stringBuilder, indentLevel + 1, false);
        } else {
            stringBuilder.append("  ".repeat(indentLevel + 1)).append("cond=");
            cond.genFormattedString(stringBuilder, indentLevel + 1, false);
            stringBuilder.append("  ".repeat(indentLevel + 1)).append("body=");
            body.genFormattedString(stringBuilder, indentLevel + 1, false);
        }
        stringBuilder.append("  ".repeat(indentLevel)).append(")\n");
    }
}
