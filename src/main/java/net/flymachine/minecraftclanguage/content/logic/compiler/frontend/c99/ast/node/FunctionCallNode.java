package net.flymachine.minecraftclanguage.content.logic.compiler.frontend.c99.ast.node;

import net.flymachine.minecraftclanguage.content.logic.compiler.frontend.c99.ast.AstVisitor;
import net.flymachine.minecraftclanguage.content.logic.compiler.frontend.c99.ast.ExpressionBoolVisitor;
import net.flymachine.minecraftclanguage.content.logic.compiler.frontend.c99.ast.ExpressionVisitor;
import net.flymachine.minecraftclanguage.content.logic.compiler.ir.TacValue;
import net.flymachine.minecraftclanguage.content.logic.errorHandle.SourceLocation;

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
    public void genFormattedString(StringBuilder stringBuilder, int indentLevel, boolean indentFirstLine) {
        if (indentFirstLine) { stringBuilder.append("  ".repeat(indentLevel)); }
        stringBuilder.append("FunctionCallNode(\n");
        stringBuilder.append("  ".repeat(indentLevel + 1)).append("function=");
        func.genFormattedString(stringBuilder, indentLevel + 1, false);
        stringBuilder.append("  ".repeat(indentLevel + 1)).append("arguments=[\n");
        for (ExpressionNode argument : args) {
            argument.genFormattedString(stringBuilder, indentLevel + 2, true);
            stringBuilder.append("  ".repeat(indentLevel + 2)).append(",\n");
        }
        stringBuilder.append("  ".repeat(indentLevel + 1)).append("]\n");
        if (expType != null) {
            stringBuilder.append("  ".repeat(indentLevel + 1)).append("expType=").append(expType).append("\n");
        }
        stringBuilder.append("  ".repeat(indentLevel)).append(")\n");
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
