package net.flymachine.minecraftclanguage.content.logic.compiler.frontend.c99.ast.node;

import net.flymachine.minecraftclanguage.content.logic.compiler.frontend.c99.ast.AstVisitor;
import net.flymachine.minecraftclanguage.content.logic.compiler.frontend.c99.ast.ExpressionBoolVisitor;
import net.flymachine.minecraftclanguage.content.logic.compiler.frontend.c99.ast.ExpressionVisitor;
import net.flymachine.minecraftclanguage.content.logic.compiler.ir.TacValue;
import net.flymachine.minecraftclanguage.content.logic.errorHandle.SourceLocation;

import java.io.PrintStream;

public final class ConditionalExpressionNode extends ExpressionNode {
    public ExpressionNode cond;
    public ExpressionNode thenExp;
    public ExpressionNode elseExp;

    public ConditionalExpressionNode(ExpressionNode cond, ExpressionNode thenExp, ExpressionNode elseExp) {
        super(SourceLocation.concat(cond.wholeLoc, elseExp.wholeLoc));
        this.cond = cond;
        this.thenExp = thenExp;
        this.elseExp = elseExp;
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

        stream.println("ConditionalExpressionNode(");

        indent(stream, indentLevel + 1);
        stream.print("cond=");
        cond.dump(stream, indentLevel + 1, false);
        stream.println(',');

        indent(stream, indentLevel + 1);
        stream.println("thenExp=");
        thenExp.dump(stream, indentLevel + 1, false);
        stream.println(',');

        indent(stream, indentLevel + 1);
        stream.println("elseExp=");
        elseExp.dump(stream, indentLevel + 1, false);
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
