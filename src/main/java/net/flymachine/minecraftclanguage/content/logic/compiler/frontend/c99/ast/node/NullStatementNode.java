package net.flymachine.minecraftclanguage.content.logic.compiler.frontend.c99.ast.node;

import net.flymachine.minecraftclanguage.content.logic.compiler.frontend.c99.ast.AstVisitor;
import net.flymachine.minecraftclanguage.content.logic.compiler.frontend.c99.ast.StatementVisitor;
import net.flymachine.minecraftclanguage.content.logic.errorHandle.SourceLocation;

public final class NullStatementNode extends StatementNode {
    // 其 Location 为分号的 Location

    public NullStatementNode(SourceLocation wholeLocation) {
        super(wholeLocation);
    }

    @Override
    public <T> T accept(AstVisitor<T> visitor) {
        return visitor.visit(this);
    }

    @Override
    public void genFormattedString(StringBuilder stringBuilder, int indentLevel, boolean indentFirstLine) {
        if (indentFirstLine) {
            stringBuilder.append("  ".repeat(indentLevel));
        }
        if (isLabeled()) {
            stringBuilder.append("NullStatementNode(");
            genFormatedStringForLabels(stringBuilder);
            stringBuilder.append(")\n");
        } else {
            stringBuilder.append("NullStatementNode()\n");
        }
    }

    @Override
    public void accept(StatementVisitor visitor) {
        visitor.visit(this);
    }
}
