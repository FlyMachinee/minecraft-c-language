package net.flymachine.minecraftclanguage.content.logic.compiler.frontend.c99.ast.node;

import net.flymachine.minecraftclanguage.content.logic.compiler.frontend.c99.ast.AstVisitor;
import net.flymachine.minecraftclanguage.content.logic.compiler.frontend.c99.ast.ExpressionBoolVisitor;
import net.flymachine.minecraftclanguage.content.logic.compiler.frontend.c99.ast.ExpressionVisitor;

import java.io.PrintStream;

public final class VariableNode extends ExpressionNode {
    public IdentifierNode id;

    public VariableNode(IdentifierNode id) {
        super(id.wholeLoc);
        this.id = id;
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
        stream.print("VariableNode(id=");
        stream.print(id.name);
        if (expType != null) {
            stream.print(", expType=");
            stream.print(expType);
        }
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
