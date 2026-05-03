package net.flymachine.minecraftclanguage.content.logic.compiler.frontend.c99.ast.node;

import net.flymachine.minecraftclanguage.content.logic.compiler.frontend.c99.ast.AstVisitor;
import net.flymachine.minecraftclanguage.content.logic.errorHandle.SourceLocation;

import java.util.List;

public final class CompoundStatementNode extends StatementNode {
    public List<BlockItemNode> blockItems;

    public CompoundStatementNode(SourceLocation wholeLocation, List<BlockItemNode> blockItems) {
        super(wholeLocation);
        this.blockItems = blockItems;
    }

    @Override
    public boolean containsActiveGotoLabel() {
        if (super.containsActiveGotoLabel()) {
            return true;
        }
        for (BlockItemNode blockItem : blockItems) {
            if (blockItem instanceof StatementNode stmtNode && stmtNode.containsActiveGotoLabel()) {
                return true;
            }
        }
        return false;
    }

    @Override
    public <T> T accept(AstVisitor<T> visitor) {
        return visitor.visit(this);
    }

    @Override
    public void genFormattedString(StringBuilder stringBuilder, int indentLevel, boolean indentFirstLine) {
        if (indentFirstLine) { stringBuilder.append("  ".repeat(indentLevel)); }
        stringBuilder.append("CompoundStatementNode(\n");
        if (!gotoLabels.isEmpty()) {
            stringBuilder.append("  ".repeat(indentLevel + 1));
            genFormatedStringForGotoLabels(stringBuilder);
            stringBuilder.append(",\n");
        }
        stringBuilder.append("  ".repeat(indentLevel + 1));
        stringBuilder.append("blockItems=[\n");
        for (BlockItemNode item : blockItems) {
            item.genFormattedString(stringBuilder, indentLevel + 2, true);
            stringBuilder.append("  ".repeat(indentLevel + 2)).append(",\n");
        }
        stringBuilder.append("  ".repeat(indentLevel + 1)).append("]\n");
        stringBuilder.append("  ".repeat(indentLevel)).append(")\n");
    }
}
