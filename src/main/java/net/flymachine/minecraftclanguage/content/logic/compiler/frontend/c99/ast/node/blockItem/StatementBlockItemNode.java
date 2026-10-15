package net.flymachine.minecraftclanguage.content.logic.compiler.frontend.c99.ast.node.blockItem;

import net.flymachine.minecraftclanguage.content.logic.compiler.frontend.c99.ast.AstVisitor;
import net.flymachine.minecraftclanguage.content.logic.compiler.frontend.c99.ast.node.AstNode;
import net.flymachine.minecraftclanguage.content.logic.compiler.frontend.c99.ast.node.statement.StatementNode;

import java.io.PrintStream;

public final class StatementBlockItemNode extends AstNode implements BlockItemNode {
    public StatementNode stmt;

    public StatementBlockItemNode(StatementNode stmt) {
        super(stmt.wholeLoc);
        this.stmt = stmt;
    }

    @Override
    public <T> T accept(AstVisitor<T> visitor) {
        return stmt.accept(visitor);
    }

    @Override
    public void dump(PrintStream stream, int indentLevel, boolean indentFirstLine) {
        stmt.dump(stream, indentLevel, indentFirstLine);
    }
}
