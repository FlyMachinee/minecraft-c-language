package net.flymachine.minecraftclanguage.content.logic.compiler.frontend.c99.ast.node;

import net.flymachine.minecraftclanguage.content.logic.compiler.frontend.c99.ast.AstVisitor;

public final class ForInitExpressionNode extends AstNode implements ForInitNode {
    public ExpressionNode expression;

    public ForInitExpressionNode(ExpressionNode expression) {
        super(expression.wholeLocation);
        this.expression = expression;
    }

    @Override
    public <T> T accept(AstVisitor<T> visitor) {
        return expression.accept(visitor);
    }

    @Override
    public void genFormattedString(StringBuilder stringBuilder, int indentLevel, boolean indentFirstLine) {
        expression.genFormattedString(stringBuilder, indentLevel, indentFirstLine);
    }
}
