package net.flymachine.minecraftclanguage.content.logic.compiler.frontend.c99.ast.node;

import net.flymachine.minecraftclanguage.content.logic.compiler.frontend.c99.ast.AstVisitor;
import net.flymachine.minecraftclanguage.content.logic.compiler.frontend.c99.ast.StatementVisitor;
import net.flymachine.minecraftclanguage.content.logic.errorHandle.SourceLocation;

import java.io.PrintStream;

public final class WhileLoopNode extends StatementNode {
    public ExpressionNode cond;
    public StatementNode body;
    public boolean isDoWhile;
    public String loopLabel;

    public WhileLoopNode(SourceLocation wholeLocation, ExpressionNode cond, StatementNode body) {
        super(wholeLocation);
        this.cond = cond;
        this.body = body;
        this.isDoWhile = false;
    }

    public WhileLoopNode(SourceLocation wholeLocation, ExpressionNode cond, StatementNode body, boolean isDoWhile) {
        super(wholeLocation);
        this.cond = cond;
        this.body = body;
        this.isDoWhile = isDoWhile;
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

        stream.println(isDoWhile ? "DoWhileLoopNode(" : "WhileLoopNode(");

        if (isLabeled()) {
            indent(stream, indentLevel + 1);
            dumpLabels(stream);
            stream.println(',');
        }

        if (loopLabel != null) {
            indent(stream, indentLevel + 1);
            stream.print("loop=");
            stream.print(loopLabel);
            stream.println(',');
        }

        if (isDoWhile) {
            indent(stream, indentLevel + 1);
            stream.print("body=");
            body.dump(stream, indentLevel + 1, false);
            stream.println(',');

            indent(stream, indentLevel + 1);
            stream.print("cond=");
            cond.dump(stream, indentLevel + 1, false);
            stream.println(',');
        } else {
            indent(stream, indentLevel + 1);
            stream.print("cond=");
            cond.dump(stream, indentLevel + 1, false);
            stream.println(',');

            indent(stream, indentLevel + 1);
            stream.print("body=");
            body.dump(stream, indentLevel + 1, false);
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
