package net.flymachine.minecraftclanguage.content.logic.compiler.frontend.c99.ast.node;

import net.flymachine.minecraftclanguage.content.logic.compiler.frontend.c99.ast.AstVisitor;

public final class StatementBlockItemNode extends AstNode implements BlockItemNode {
    public StatementNode statement;

    public StatementBlockItemNode(StatementNode statement) {
        super(statement.wholeLocation);
        this.statement = statement;
    }

    @Override
    public <T> T accept(AstVisitor<T> visitor) {
        return statement.accept(visitor);
    }

    @Override
    public void genFormattedString(StringBuilder stringBuilder, int indentLevel, boolean indentFirstLine) {
        statement.genFormattedString(stringBuilder, indentLevel, indentFirstLine);
    }
}
