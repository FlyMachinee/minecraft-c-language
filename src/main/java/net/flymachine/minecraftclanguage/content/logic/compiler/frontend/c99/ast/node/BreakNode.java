package net.flymachine.minecraftclanguage.content.logic.compiler.frontend.c99.ast.node;

import net.flymachine.minecraftclanguage.content.logic.compiler.frontend.c99.ast.AstVisitor;
import net.flymachine.minecraftclanguage.content.logic.compiler.frontend.c99.ast.StatementVisitor;
import net.flymachine.minecraftclanguage.content.logic.errorHandle.SourceLocation;

public final class BreakNode extends StatementNode {
    public String loopOrSwitchLabel;

    public BreakNode(SourceLocation wholeLocation) {
        super(wholeLocation);
    }

    @Override
    public <T> T accept(AstVisitor<T> visitor) {
        return visitor.visit(this);
    }

    @Override
    public void genFormattedString(StringBuilder stringBuilder, int indentLevel, boolean indentFirstLine) {
        if (indentFirstLine) { stringBuilder.append("  ".repeat(indentLevel)); }
        if (loopOrSwitchLabel != null) {
            stringBuilder.append("BreakNode(loop/switch=").append(loopOrSwitchLabel).append(")\n");
        } else {
            stringBuilder.append("BreakNode()\n");
        }
    }

    @Override
    public void accept(StatementVisitor visitor) {
        visitor.visit(this);
    }
}
