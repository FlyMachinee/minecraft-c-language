package net.flymachine.minecraftclanguage.content.logic.compiler.frontend.c99.ast.node;

import net.flymachine.minecraftclanguage.content.logic.compiler.frontend.c99.ast.AstVisitor;
import net.flymachine.minecraftclanguage.content.logic.errorHandle.SourceLocation;
import org.jetbrains.annotations.Nullable;

public final class DeclarationNode extends AstNode implements ExternalDeclarationNode {
    public @Nullable StorageClassSpecifierNode storageClass;
    public TypeNode type;
    public IdentifierNode identifier;
    public @Nullable ExpressionNode initializer;

    public DeclarationNode(
        SourceLocation wholeLocation, @Nullable StorageClassSpecifierNode storageClass, TypeNode type,
        IdentifierNode identifier, @Nullable ExpressionNode initializer) {

        super(wholeLocation);
        this.storageClass = storageClass;
        this.type = type;
        this.identifier = identifier;
        this.initializer = initializer;
    }

    public DeclarationNode(
        SourceLocation wholeLocation, @Nullable StorageClassSpecifierNode storageClass, TypeNode type,
        IdentifierNode identifier) {

        super(wholeLocation);
        this.storageClass = storageClass;
        this.type = type;
        this.identifier = identifier;
        this.initializer = null;
    }

    public DeclarationNode(SourceLocation wholeLocation, TypeNode type, IdentifierNode identifier) {
        super(wholeLocation);
        this.storageClass = null;
        this.type = type;
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
        stringBuilder.append("  ".repeat(indentLevel + 1)).append("storage=");
        if (storageClass != null) {
            storageClass.genFormattedString(stringBuilder, indentLevel + 1, false);
            stringBuilder.append("\n");
        } else {
            stringBuilder.append("null\n");
        }
        stringBuilder.append("  ".repeat(indentLevel + 1)).append("type=");
        type.genFormattedString(stringBuilder);
        stringBuilder.append(",\n");
        stringBuilder.append("  ".repeat(indentLevel + 1)).append("id=\"").append(identifier.id).append("\",\n");
        stringBuilder.append("  ".repeat(indentLevel + 1)).append("init=");
        if (initializer != null) {
            initializer.genFormattedString(stringBuilder, indentLevel + 1, false);
        } else {
            stringBuilder.append("null\n");
        }
        stringBuilder.append("  ".repeat(indentLevel)).append(")\n");
    }
}
