package net.flymachine.minecraftclanguage.content.logic.compiler.frontend.c99.ast.node;

import net.flymachine.minecraftclanguage.content.logic.compiler.frontend.c99.ast.AstVisitor;
import net.flymachine.minecraftclanguage.content.logic.errorHandle.SourceLocation;

public final class GotoNode extends StatementNode {
    public IdentifierNode target;
    public SourceLocation gotoLocation;

    public GotoNode(SourceLocation gotoLocation, IdentifierNode target) {
        super(SourceLocation.concat(gotoLocation, target.wholeLocation));
        this.target = target;
        this.gotoLocation = gotoLocation;
    }

    @Override
    public <T> T accept(AstVisitor<T> visitor) {
        return visitor.visit(this);
    }

    @Override
    public void genFormattedString(StringBuilder stringBuilder, int indentLevel, boolean indentFirstLine) {
        if (indentFirstLine) { stringBuilder.append("  ".repeat(indentLevel)); }
        stringBuilder.append("GotoNode(\n");
        if (isLabeled()) {
            stringBuilder.append("  ".repeat(indentLevel + 1));
            genFormatedStringForLabels(stringBuilder);
            stringBuilder.append(",\n");
        }
        stringBuilder.append("  ".repeat(indentLevel + 1)).append("target=");
        target.genFormattedString(stringBuilder, indentLevel + 1, false);
        stringBuilder.append("  ".repeat(indentLevel)).append(")\n");
    }
}
