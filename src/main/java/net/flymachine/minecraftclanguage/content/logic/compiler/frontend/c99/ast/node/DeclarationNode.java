package net.flymachine.minecraftclanguage.content.logic.compiler.frontend.c99.ast.node;

import net.flymachine.minecraftclanguage.content.logic.compiler.frontend.c99.ast.AstVisitor;

public class DeclarationNode implements BlockItemNode {
    public String identifier;
    public ExpressionNode initializer;

    public DeclarationNode(String identifier, ExpressionNode initializer) {
        this.identifier = identifier;
        this.initializer = initializer;
    }

    public DeclarationNode(String identifier) {
        this.identifier = identifier;
        this.initializer = null;
    }

    @Override
    public <T> T accept(AstVisitor<T> visitor) {
        return visitor.visit(this);
    }

    @Override
    public void genFormattedString(StringBuilder stringBuilder, int indentLevel, boolean indentFirstLine) {
        if (indentFirstLine) { stringBuilder.append("  ".repeat(indentLevel)); }
        stringBuilder.append("DeclarationNode(\n");
        stringBuilder.append("  ".repeat(indentLevel + 1)).append("id=\"").append(identifier).append("\",\n");
        stringBuilder.append("  ".repeat(indentLevel + 1)).append("init=");
        if (initializer != null) {
            initializer.genFormattedString(stringBuilder, indentLevel + 1, false);
        } else {
            stringBuilder.append("null\n");
        }
        stringBuilder.append("  ".repeat(indentLevel)).append(")\n");
    }
}
