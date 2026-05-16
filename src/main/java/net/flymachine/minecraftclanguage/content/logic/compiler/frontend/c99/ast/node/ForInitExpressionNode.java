package net.flymachine.minecraftclanguage.content.logic.compiler.frontend.c99.ast.node;

import net.flymachine.minecraftclanguage.content.logic.compiler.frontend.c99.ast.AstVisitor;

public final class ForInitExpressionNode extends AstNode implements ForInitNode {
    public ExpressionNode exp;

    public ForInitExpressionNode(ExpressionNode exp) {
        super(exp.wholeLoc);
        this.exp = exp;
    }

    @Override
    public <T> T accept(AstVisitor<T> visitor) {
        return exp.accept(visitor);
    }

    @Override
    public void genFormattedString(StringBuilder stringBuilder, int indentLevel, boolean indentFirstLine) {
        exp.genFormattedString(stringBuilder, indentLevel, indentFirstLine);
    }
}
