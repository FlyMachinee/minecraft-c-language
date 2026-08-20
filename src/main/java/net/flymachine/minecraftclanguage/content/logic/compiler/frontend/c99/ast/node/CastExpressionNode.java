package net.flymachine.minecraftclanguage.content.logic.compiler.frontend.c99.ast.node;

import net.flymachine.minecraftclanguage.content.logic.compiler.frontend.c99.ast.AstVisitor;
import net.flymachine.minecraftclanguage.content.logic.compiler.frontend.c99.ast.ExpressionBoolVisitor;
import net.flymachine.minecraftclanguage.content.logic.compiler.frontend.c99.ast.ExpressionVisitor;
import net.flymachine.minecraftclanguage.content.logic.compiler.ir.TacValue;
import net.flymachine.minecraftclanguage.content.logic.errorHandle.SourceLocation;

import java.io.PrintStream;

public final class CastExpressionNode extends ExpressionNode {
    public TypeNode targetType;
    public ExpressionNode exp;

    public CastExpressionNode(SourceLocation wholeLocation, TypeNode targetType, ExpressionNode exp) {
        super(wholeLocation);
        this.targetType = targetType;
        this.exp = exp;
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

        stream.println("CastNode(");

        indent(stream, indentLevel + 1);
        stream.print("targetType=");
        targetType.dump(stream);
        stream.println(',');

        indent(stream, indentLevel + 1);
        stream.print("exp=");
        exp.dump(stream, indentLevel + 1, false);
        stream.println(',');

        dumpExpType(stream, indentLevel + 1);

        indent(stream, indentLevel);
        stream.print(')');
    }

    @Override
    public ExpressionVisitor.ExpEvalResult accept(ExpressionVisitor visitor) {
        return visitor.visit(this);
    }

    @Override
    public ExpressionBoolVisitor.BoolGenResult accept(
        ExpressionBoolVisitor visitor, String jumpTarget, boolean inverse) {
        return visitor.visit(this, jumpTarget, inverse);
    }
}
