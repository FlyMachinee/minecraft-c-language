package net.flymachine.minecraftclanguage.content.logic.compiler.frontend.c99.ast.node;

import net.flymachine.minecraftclanguage.content.logic.compiler.frontend.c99.ast.AstVisitor;
import net.flymachine.minecraftclanguage.content.logic.compiler.frontend.c99.ast.ExpressionBoolVisitor;
import net.flymachine.minecraftclanguage.content.logic.compiler.frontend.c99.ast.ExpressionVisitor;
import net.flymachine.minecraftclanguage.content.logic.errorHandle.SourceLocation;

import java.io.PrintStream;

public final class BinaryExpressionNode extends ExpressionNode {
    public BinaryOperatorNode op;
    public ExpressionNode lhs;
    public ExpressionNode rhs;

    public BinaryExpressionNode(BinaryOperatorNode op, ExpressionNode lhs, ExpressionNode rhs) {
        super(SourceLocation.concat(lhs.wholeLoc, rhs.wholeLoc));
        this.op = op;
        this.lhs = lhs;
        this.rhs = rhs;
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

        stream.println("BinaryExpressionNode(");

        indent(stream, indentLevel + 1);
        stream.print("op=");
        op.dump(stream);
        stream.println(',');

        indent(stream, indentLevel + 1);
        stream.print("lhs=");
        lhs.dump(stream, indentLevel + 1, false);
        stream.println(',');

        indent(stream, indentLevel + 1);
        stream.print("rhs=");
        rhs.dump(stream, indentLevel + 1, false);
        stream.println(',');

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
