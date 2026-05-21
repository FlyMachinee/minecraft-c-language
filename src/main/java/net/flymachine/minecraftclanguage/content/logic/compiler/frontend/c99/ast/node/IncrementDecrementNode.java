package net.flymachine.minecraftclanguage.content.logic.compiler.frontend.c99.ast.node;

import net.flymachine.minecraftclanguage.content.logic.compiler.frontend.c99.ast.AstVisitor;
import net.flymachine.minecraftclanguage.content.logic.compiler.frontend.c99.ast.ExpressionBoolVisitor;
import net.flymachine.minecraftclanguage.content.logic.compiler.frontend.c99.ast.ExpressionVisitor;
import net.flymachine.minecraftclanguage.content.logic.compiler.ir.TacValue;
import net.flymachine.minecraftclanguage.content.logic.errorHandle.SourceLocation;

import java.io.PrintStream;

public final class IncrementDecrementNode extends ExpressionNode {
    public boolean isIncrement;
    public boolean isPrefix;
    public ExpressionNode operand;
    public SourceLocation operatorLoc;

    public IncrementDecrementNode(
        SourceLocation operatorLoc, boolean isIncrement, boolean isPrefix, ExpressionNode operand) {

        super(SourceLocation.concat(operatorLoc, operand.wholeLoc));
        this.operatorLoc = operatorLoc;
        this.isIncrement = isIncrement;
        this.isPrefix = isPrefix;
        this.operand = operand;
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

        stream.println(isIncrement ? "IncrementNode(" : "DecrementNode(");

        indent(stream, indentLevel + 1);
        stream.print("prefix=");
        stream.print(isPrefix);
        stream.println(',');

        indent(stream, indentLevel + 1);
        stream.print("operand=");
        operand.dump(stream, indentLevel + 1, false);
        stream.println(',');

        dumpExpType(stream, indentLevel + 1);

        indent(stream, indentLevel);
        stream.print(')');
    }

    @Override
    public TacValue accept(ExpressionVisitor visitor) {
        return visitor.visit(this);
    }

    @Override
    public ExpressionBoolVisitor.BoolGenResult accept(
        ExpressionBoolVisitor visitor, String jumpTarget, boolean inverse) {
        return visitor.visit(this, jumpTarget, inverse);
    }
}
