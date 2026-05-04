package net.flymachine.minecraftclanguage.content.logic.compiler.frontend.c99.ast.node;

import net.flymachine.minecraftclanguage.content.logic.compiler.frontend.c99.ast.AstVisitor;
import net.flymachine.minecraftclanguage.content.logic.errorHandle.SourceLocation;
import org.jetbrains.annotations.Nullable;

public final class IfStatementNode extends StatementNode {
    public ExpressionNode cond;
    public StatementNode thenStmt;
    public @Nullable StatementNode elseStmt;

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
    public boolean containsActiveGotoLabel() {
        return super.containsActiveGotoLabel() || thenStmt.containsActiveGotoLabel() ||
               (elseStmt != null && elseStmt.containsActiveGotoLabel());
    }

    @Override
    public <T> T accept(AstVisitor<T> visitor) {
        return visitor.visit(this);
    }

    @Override
    public void genFormattedString(StringBuilder stringBuilder, int indentLevel, boolean indentFirstLine) {
        if (indentFirstLine) { stringBuilder.append("  ".repeat(indentLevel)); }
        stringBuilder.append("IfStatementNode(\n");
        if (!gotoLabels.isEmpty()) {
            stringBuilder.append("  ".repeat(indentLevel + 1));
            genFormatedStringForGotoLabels(stringBuilder);
            stringBuilder.append(",\n");
        }
        stringBuilder.append("  ".repeat(indentLevel + 1)).append("cond=");
        cond.genFormattedString(stringBuilder, indentLevel + 1, false);
        stringBuilder.append("  ".repeat(indentLevel + 1)).append("thenStmt=");
        thenStmt.genFormattedString(stringBuilder, indentLevel + 1, false);
        if (elseStmt != null) {
            stringBuilder.append("  ".repeat(indentLevel + 1)).append("elseStmt=");
            elseStmt.genFormattedString(stringBuilder, indentLevel + 1, false);
        }
        stringBuilder.append("  ".repeat(indentLevel)).append(")\n");
    }
}
