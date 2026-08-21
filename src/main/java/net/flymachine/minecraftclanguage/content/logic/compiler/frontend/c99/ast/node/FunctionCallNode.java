package net.flymachine.minecraftclanguage.content.logic.compiler.frontend.c99.ast.node;

import net.flymachine.minecraftclanguage.content.logic.compiler.frontend.c99.ast.AstVisitor;
import net.flymachine.minecraftclanguage.content.logic.compiler.frontend.c99.ast.ExpressionBoolVisitor;
import net.flymachine.minecraftclanguage.content.logic.compiler.frontend.c99.ast.ExpressionVisitor;
import net.flymachine.minecraftclanguage.content.logic.errorHandle.SourceLocation;

import java.io.PrintStream;
import java.util.List;

public final class FunctionCallNode extends ExpressionNode {
    public ExpressionNode func;
    public List<ExpressionNode> args;

    public FunctionCallNode(SourceLocation wholeLocation, ExpressionNode func, List<ExpressionNode> args) {
        super(wholeLocation);
        this.func = func;
        this.args = args;
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

        stream.println("FunctionCallNode(");

        indent(stream, indentLevel + 1);
        stream.print("func=");
        func.dump(stream, indentLevel + 1, false);
        stream.println(',');

        indent(stream, indentLevel + 1);
        if (!args.isEmpty()) {
            stream.println("args=[");
            for (ExpressionNode argument : args) {
                argument.dump(stream, indentLevel + 2, true);
                stream.println(',');
            }
            indent(stream, indentLevel + 1);
            stream.println("],");
        } else {
            stream.println("args=[],");
        }

        dumpExpType(stream, indentLevel + 1);

        indent(stream, indentLevel);
        stream.print(')');
    }

    @Override
    public <T> T accept(ExpressionVisitor<T> visitor) {
        return visitor.visit(this);
    }

    @Override
    public ExpressionBoolVisitor.BoolGenResult accept(
        ExpressionBoolVisitor visitor, String jumpTarget, boolean inverse) {
        return visitor.visit(this, jumpTarget, inverse);
    }
}
