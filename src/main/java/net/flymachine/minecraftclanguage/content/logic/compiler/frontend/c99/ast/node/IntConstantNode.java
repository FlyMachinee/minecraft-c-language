package net.flymachine.minecraftclanguage.content.logic.compiler.frontend.c99.ast.node;

public class IntConstantNode implements ExpressionNode {
    public int value;

    public IntConstantNode(int value) {
        this.value = value;
    }

    @Override
    public void genFormattedString(StringBuilder stringBuilder, int indentLevel, boolean indentFirstLine) {
        if (indentFirstLine) { stringBuilder.append("  ".repeat(indentLevel)); }
        stringBuilder.append("IntConstantNode(value=").append(value).append(")\n");
    }
}

