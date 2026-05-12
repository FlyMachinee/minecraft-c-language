package net.flymachine.minecraftclanguage.content.logic.compiler.frontend.c99.ast.node;

import net.flymachine.minecraftclanguage.content.logic.compiler.frontend.c99.ast.AstVisitor;
import net.flymachine.minecraftclanguage.content.logic.errorHandle.SourceLocation;

import java.util.List;

public final class FunctionCallNode extends ExpressionNode {
    public ExpressionNode function;
    public List<ExpressionNode> arguments;

    public FunctionCallNode(SourceLocation wholeLocation, ExpressionNode function, List<ExpressionNode> arguments) {
        super(wholeLocation);
        this.function = function;
        this.arguments = arguments;
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
        function.genFormattedString(stringBuilder, indentLevel + 1, false);
        stringBuilder.append("  ".repeat(indentLevel + 1)).append("arguments=[\n");
        for (ExpressionNode argument : arguments) {
            argument.genFormattedString(stringBuilder, indentLevel + 2, true);
            stringBuilder.append("  ".repeat(indentLevel + 2)).append(",\n");
        }
        stringBuilder.append("  ".repeat(indentLevel + 1)).append("]\n");
        stringBuilder.append("  ".repeat(indentLevel)).append(")\n");
    }
}
