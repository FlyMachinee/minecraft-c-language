package net.flymachine.minecraftclanguage.content.logic.compiler.frontend.c99.ast.node;

import net.flymachine.minecraftclanguage.content.logic.compiler.frontend.c99.ast.AstVisitor;
import net.flymachine.minecraftclanguage.content.logic.errorHandle.SourceLocation;
import org.jetbrains.annotations.Nullable;

public final class DeclarationNode extends AstNode implements ExternalDeclarationNode {
    public @Nullable StorageClassSpecifierNode storageClass;
    public TypeNode type;
    public IdentifierNode id;
    public @Nullable ExpressionNode init;

    public DeclarationNode(
        SourceLocation wholeLocation, @Nullable StorageClassSpecifierNode storageClass, TypeNode type,
        IdentifierNode id, @Nullable ExpressionNode init) {

        super(wholeLocation);
        this.storageClass = storageClass;
        this.type = type;
        this.id = id;
        this.init = init;
    }

    public DeclarationNode(
        SourceLocation wholeLocation, @Nullable StorageClassSpecifierNode storageClass, TypeNode type,
        IdentifierNode id) {

        super(wholeLocation);
        this.storageClass = storageClass;
        this.type = type;
        this.id = id;
        this.init = null;
    }

    public DeclarationNode(SourceLocation wholeLocation, TypeNode type, IdentifierNode id) {
        super(wholeLocation);
        this.storageClass = null;
        this.type = type;
        this.id = id;
        this.init = null;
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
        stringBuilder.append("  ".repeat(indentLevel + 1)).append("id=\"").append(id.id).append("\",\n");
        stringBuilder.append("  ".repeat(indentLevel + 1)).append("init=");
        if (init != null) {
            init.genFormattedString(stringBuilder, indentLevel + 1, false);
        } else {
            stringBuilder.append("null\n");
        }
        stringBuilder.append("  ".repeat(indentLevel)).append(")\n");
    }
}
