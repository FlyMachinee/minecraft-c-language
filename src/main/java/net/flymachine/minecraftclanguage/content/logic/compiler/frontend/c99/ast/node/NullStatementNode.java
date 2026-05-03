package net.flymachine.minecraftclanguage.content.logic.compiler.frontend.c99.ast.node;

import net.flymachine.minecraftclanguage.content.logic.compiler.frontend.c99.ast.AstVisitor;
import net.flymachine.minecraftclanguage.content.logic.errorHandle.SourceLocation;

public class NullStatementNode extends StatementNode {

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
        if (!gotoLabels.isEmpty()) {
            stringBuilder.append("NullStatementNode(");
            genFormatedStringForGotoLabels(stringBuilder);
            stringBuilder.append(")\n");
        } else {
            stringBuilder.append("NullStatementNode()\n");
        }
    }
}
