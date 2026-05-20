package net.flymachine.minecraftclanguage.content.logic.compiler.frontend.c99.ast.node;

import net.flymachine.minecraftclanguage.content.logic.compiler.frontend.c99.ast.AstVisitor;
import net.flymachine.minecraftclanguage.content.logic.compiler.frontend.c99.ast.StatementVisitor;
import net.flymachine.minecraftclanguage.content.logic.errorHandle.SourceLocation;

public final class GotoNode extends StatementNode {
    public IdentifierNode target;
    /**
     * goto 关键字的 Location
     */
    public SourceLocation gotoLoc;

    public GotoNode(SourceLocation gotoLoc, IdentifierNode target) {
        super(SourceLocation.concat(gotoLoc, target.wholeLoc));
        this.target = target;
        this.gotoLoc = gotoLoc;
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

    @Override
    public void accept(StatementVisitor visitor) {
        visitor.visit(this);
    }
}
