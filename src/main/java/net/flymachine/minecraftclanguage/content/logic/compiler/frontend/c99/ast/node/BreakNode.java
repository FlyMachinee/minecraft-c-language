package net.flymachine.minecraftclanguage.content.logic.compiler.frontend.c99.ast.node;

import net.flymachine.minecraftclanguage.content.logic.compiler.frontend.c99.ast.AstVisitor;
import net.flymachine.minecraftclanguage.content.logic.compiler.frontend.c99.ast.StatementVisitor;
import net.flymachine.minecraftclanguage.content.logic.errorHandle.SourceLocation;

import java.io.PrintStream;

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
    public void dump(PrintStream stream, int indentLevel, boolean indentFirstLine) {
        if (indentFirstLine) {
            indent(stream, indentLevel);
        }
        stream.println("BreakNode(");

        if (isLabeled()) {
            indent(stream, indentLevel + 1);
            dumpLabels(stream);
            stream.println(',');
        }

        if (loopOrSwitchLabel != null) {
            indent(stream, indentLevel + 1);
            stream.append("loop/switch=").append(loopOrSwitchLabel);
            stream.println(',');
        }

        indent(stream, indentLevel);
        stream.print(')');
    }

    @Override
    public void accept(StatementVisitor visitor) {
        visitor.visit(this);
    }
}
