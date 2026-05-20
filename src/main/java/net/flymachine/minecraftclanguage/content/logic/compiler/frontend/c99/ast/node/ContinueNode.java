package net.flymachine.minecraftclanguage.content.logic.compiler.frontend.c99.ast.node;

import net.flymachine.minecraftclanguage.content.logic.compiler.frontend.c99.ast.AstVisitor;
import net.flymachine.minecraftclanguage.content.logic.compiler.frontend.c99.ast.StatementVisitor;
import net.flymachine.minecraftclanguage.content.logic.errorHandle.SourceLocation;

public final class ContinueNode extends StatementNode {
    public String loopLabel;

    public ContinueNode(SourceLocation wholeLocation) {
        super(wholeLocation);
    }

    @Override
    public <T> T accept(AstVisitor<T> visitor) {
        return visitor.visit(this);
    }

    @Override
    public void genFormattedString(StringBuilder stringBuilder, int indentLevel, boolean indentFirstLine) {
        if (indentFirstLine) { stringBuilder.append("  ".repeat(indentLevel)); }
        if (loopLabel != null) {
            stringBuilder.append("ContinueNode(loop=").append(loopLabel).append(")\n");
        } else {
            stringBuilder.append("ContinueNode()\n");
        }
    }

    @Override
    public void accept(StatementVisitor visitor) {
        visitor.visit(this);
    }
}
