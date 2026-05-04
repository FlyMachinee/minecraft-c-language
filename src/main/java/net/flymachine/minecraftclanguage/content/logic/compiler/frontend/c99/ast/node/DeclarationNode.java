package net.flymachine.minecraftclanguage.content.logic.compiler.frontend.c99.ast.node;

import net.flymachine.minecraftclanguage.content.logic.compiler.frontend.c99.ast.AstVisitor;
import net.flymachine.minecraftclanguage.content.logic.errorHandle.SourceLocation;
import org.jetbrains.annotations.Nullable;

public final class DeclarationNode extends BlockItemNode implements ForInitNode {
    public IdentifierNode variable;
    public @Nullable ExpressionNode initializer;

    public DeclarationNode(IdentifierNode variable, ExpressionNode initializer) {
        super(SourceLocation.concat(variable.wholeLocation, initializer.wholeLocation));
        this.variable = variable;
        this.initializer = initializer;
    }

    public DeclarationNode(IdentifierNode variable) {
        super(variable.wholeLocation);
        this.variable = variable;
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
        stringBuilder.append("  ".repeat(indentLevel + 1)).append("id=\"").append(variable.id).append("\",\n");
        stringBuilder.append("  ".repeat(indentLevel + 1)).append("init=");
        if (initializer != null) {
            initializer.genFormattedString(stringBuilder, indentLevel + 1, false);
        } else {
            stringBuilder.append("null\n");
        }
        stringBuilder.append("  ".repeat(indentLevel)).append(")\n");
    }
}
