package net.flymachine.minecraftclanguage.content.logic.compiler.frontend.c99.ast.node;

import net.flymachine.minecraftclanguage.content.logic.compiler.frontend.c99.ast.AstVisitor;
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
