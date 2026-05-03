package net.flymachine.minecraftclanguage.content.logic.compiler.frontend.c99.ast.node;

import net.flymachine.minecraftclanguage.content.logic.compiler.frontend.c99.ast.AstVisitor;
import net.flymachine.minecraftclanguage.content.logic.errorHandle.SourceLocation;

public final class IfStatementNode extends StatementNode {
    public ExpressionNode cond;
    public StatementNode thenStmt;
    public StatementNode elseStmt;

    public IfStatementNode(ExpressionNode cond, StatementNode thenStmt, StatementNode elseStmt) {
        super(SourceLocation.concat(cond.wholeLocation, elseStmt.wholeLocation));
        this.cond = cond;
        this.thenStmt = thenStmt;
        this.elseStmt = elseStmt;
    }

    public IfStatementNode(ExpressionNode cond, StatementNode thenStmt) {
        super(SourceLocation.concat(cond.wholeLocation, thenStmt.wholeLocation));
        this.cond = cond;
        this.thenStmt = thenStmt;
        this.elseStmt = null;
    }

    @Override
    public <T> T accept(AstVisitor<T> visitor) {
        return visitor.visit(this);
    }

    @Override
    public void genFormattedString(StringBuilder stringBuilder, int indentLevel, boolean indentFirstLine) {

    }
}
