package net.flymachine.minecraftclanguage.content.logic.compiler.frontend.c99.ast.node;

import net.flymachine.minecraftclanguage.content.logic.compiler.frontend.c99.ast.AstVisitor;
import net.flymachine.minecraftclanguage.content.logic.compiler.frontend.c99.ast.StatementVisitor;
import net.flymachine.minecraftclanguage.content.logic.errorHandle.SourceLocation;

import java.io.PrintStream;
import java.util.List;

public final class CompoundStatementNode extends StatementNode {
    public List<BlockItemNode> blockItems;

    public CompoundStatementNode(SourceLocation wholeLocation, List<BlockItemNode> blockItems) {
        super(wholeLocation);
        this.blockItems = blockItems;
    }

    @Override
    public boolean containsActiveLabel() {
        if (super.containsActiveLabel()) {
            return true;
        }
        for (BlockItemNode blockItem : blockItems) {
            if (blockItem instanceof StatementBlockItemNode stmtItem && stmtItem.stmt.containsActiveLabel()) {
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
    public void dump(PrintStream stream, int indentLevel, boolean indentFirstLine) {
        if (indentFirstLine) {
            indent(stream, indentLevel);
        }

        stream.println("CompoundStatementNode(");

        if (isLabeled()) {
            indent(stream, indentLevel + 1);
            dumpLabels(stream);
            stream.println(',');
        }

        indent(stream, indentLevel + 1);
        stream.println("blockItems=[");

        for (BlockItemNode item : blockItems) {
            // item.genFormattedString(stringBuilder, indentLevel + 2, true);
            item.dump(stream, indentLevel + 2, true);
            stream.println(',');
        }

        indent(stream, indentLevel + 1);
        stream.println("],");
        indent(stream, indentLevel);
        stream.print(')');
    }

    @Override
    public void accept(StatementVisitor visitor) {
        visitor.visit(this);
    }
}
