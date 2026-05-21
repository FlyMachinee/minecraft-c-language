package net.flymachine.minecraftclanguage.content.logic.compiler.frontend.c99.ast.node;

import net.flymachine.minecraftclanguage.content.logic.compiler.frontend.c99.ast.AstVisitor;
import net.flymachine.minecraftclanguage.content.logic.compiler.frontend.c99.ast.StatementVisitor;
import net.flymachine.minecraftclanguage.content.logic.errorHandle.SourceLocation;
import org.jetbrains.annotations.Nullable;

import java.io.PrintStream;

public final class IfStatementNode extends StatementNode {
    public ExpressionNode cond;
    public StatementNode thenStmt;
    public @Nullable StatementNode elseStmt;

    public IfStatementNode(ExpressionNode cond, StatementNode thenStmt, StatementNode elseStmt) {
        super(SourceLocation.concat(cond.wholeLoc, elseStmt.wholeLoc));
        this.cond = cond;
        this.thenStmt = thenStmt;
        this.elseStmt = elseStmt;
    }

    public IfStatementNode(ExpressionNode cond, StatementNode thenStmt) {
        super(SourceLocation.concat(cond.wholeLoc, thenStmt.wholeLoc));
        this.cond = cond;
        this.thenStmt = thenStmt;
        this.elseStmt = null;
    }

    @Override
    public boolean containsActiveLabel() {
        return super.containsActiveLabel() || thenStmt.containsActiveLabel() ||
               (elseStmt != null && elseStmt.containsActiveLabel());
    }

    @Override
    public <T> T accept(AstVisitor<T> visitor) {
        return visitor.visit(this);
    }

    @Override
    public void dump(PrintStream stream, int indentLevel, boolean indentFirstLine) {
        if (indentFirstLine) {
            indent(stream, indentLevel);
        }

        stream.println("IfStatementNode(");

        if (isLabeled()) {
            indent(stream, indentLevel + 1);
            dumpLabels(stream);
            stream.println(',');
        }

        indent(stream, indentLevel + 1);
        stream.print("cond=");
        cond.dump(stream, indentLevel + 1, false);
        stream.println(',');

        indent(stream, indentLevel + 1);
        stream.print("thenStmt=");
        thenStmt.dump(stream, indentLevel + 1, false);
        stream.println(',');

        if (elseStmt != null) {
            indent(stream, indentLevel + 1);
            stream.print("elseStmt=");
            elseStmt.dump(stream, indentLevel + 1, false);
            stream.println(',');
        }

        indent(stream, indentLevel);
        stream.print(')');
    }

    @Override
    public void accept(StatementVisitor visitor) {
        visitor.visit(this);
    }
}
