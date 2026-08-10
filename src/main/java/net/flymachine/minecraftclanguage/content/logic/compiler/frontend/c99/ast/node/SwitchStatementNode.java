package net.flymachine.minecraftclanguage.content.logic.compiler.frontend.c99.ast.node;

import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import net.flymachine.minecraftclanguage.content.logic.compiler.frontend.c99.ast.AstVisitor;
import net.flymachine.minecraftclanguage.content.logic.compiler.frontend.c99.ast.StatementVisitor;
import net.flymachine.minecraftclanguage.content.logic.errorHandle.SourceLocation;

import java.io.PrintStream;

public final class SwitchStatementNode extends StatementNode {
    public ExpressionNode exp;
    public StatementNode body;
    public String switchLabel;
    public Long2ObjectMap<CaseLabelInfo> caseValues = new Long2ObjectOpenHashMap<>();
    public DefaultLabelInfo defaultLabel;

    public SwitchStatementNode(ExpressionNode exp, StatementNode body) {
        super(SourceLocation.concat(exp.wholeLoc, body.wholeLoc));
        this.exp = exp;
        this.body = body;
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

        stream.println("SwitchStatementNode(");

        if (isLabeled()) {
            indent(stream, indentLevel + 1);
            dumpLabels(stream);
            stream.println(',');
        }

        if (switchLabel != null) {
            indent(stream, indentLevel + 1);
            stream.print("switch=");
            stream.print(switchLabel);
            stream.println(',');
        }

        indent(stream, indentLevel + 1);
        stream.print("exp=");
        exp.dump(stream, indentLevel + 1, false);
        stream.println(',');

        indent(stream, indentLevel + 1);
        stream.print("body=");
        body.dump(stream, indentLevel + 1, false);
        stream.println(',');

        indent(stream, indentLevel);
        stream.print(')');
    }

    @Override
    public void accept(StatementVisitor visitor) {
        visitor.visit(this);
    }
}
