package net.flymachine.minecraftclanguage.content.logic.compiler.frontend.c99.ast.node;

import net.flymachine.minecraftclanguage.content.logic.compiler.frontend.c99.ast.AstVisitor;
import net.flymachine.minecraftclanguage.content.logic.compiler.frontend.c99.ast.StatementVisitor;
import net.flymachine.minecraftclanguage.content.logic.errorHandle.SourceLocation;
import org.jetbrains.annotations.Nullable;

import java.io.PrintStream;

public final class ForLoopNode extends StatementNode {
    public @Nullable ForInitNode init;
    public @Nullable ExpressionNode cond;
    public @Nullable ExpressionNode step;
    public StatementNode body;
    public String loopLabel;

    public ForLoopNode(
        SourceLocation wholeLocation, @Nullable ForInitNode init,
        @Nullable ExpressionNode cond, @Nullable ExpressionNode step, StatementNode body) {
        super(wholeLocation);
        this.init = init;
        this.cond = cond;
        this.step = step;
        this.body = body;
    }

    @Override
    public boolean containsActiveLabel() {
        if (super.containsActiveLabel()) {
            return true;
        }
        return body.containsActiveLabel();
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

        stream.println("ForLoopNode(");

        if (isLabeled()) {
            indent(stream, indentLevel + 1);
            dumpLabels(stream);
            stream.println(',');
        }
        if (loopLabel != null) {
            indent(stream, indentLevel + 1);
            stream.append("loop=").append(loopLabel).append(',');
            stream.println();
        }
        if (init != null) {
            indent(stream, indentLevel + 1);
            stream.print("init=");
            init.dump(stream, indentLevel + 1, false);
            stream.println(',');
        }
        if (cond != null) {
            indent(stream, indentLevel + 1);
            stream.print("cond=");
            cond.dump(stream, indentLevel + 1, false);
            stream.println(',');
        }
        if (step != null) {
            indent(stream, indentLevel + 1);
            stream.print("step=");
            step.dump(stream, indentLevel + 1, false);
            stream.println(',');
        }

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
