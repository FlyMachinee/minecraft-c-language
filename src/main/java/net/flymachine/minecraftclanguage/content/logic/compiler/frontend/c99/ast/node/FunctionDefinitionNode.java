package net.flymachine.minecraftclanguage.content.logic.compiler.frontend.c99.ast.node;

import net.flymachine.minecraftclanguage.content.logic.compiler.frontend.c99.ast.AstVisitor;
import net.flymachine.minecraftclanguage.content.logic.errorHandle.SourceLocation;
import org.jetbrains.annotations.Nullable;

public final class FunctionDefinitionNode extends AstNode implements ExternalDeclarationNode {
    public IdentifierNode id;
    public TypeNode funcType;
    public @Nullable StorageClassSpecifierNode storageClass;
    public CompoundStatementNode body;

    public FunctionDefinitionNode(
        SourceLocation wholeLocation, IdentifierNode id, TypeNode funcType,
        @Nullable StorageClassSpecifierNode storageClass, CompoundStatementNode body) {

        super(wholeLocation);
        this.id = id;
        this.funcType = funcType;
        this.storageClass = storageClass;
        this.body = body;
    }

    @Override
    public <T> T accept(AstVisitor<T> visitor) {
        return visitor.visit(this);
    }

    @Override
    public void genFormattedString(StringBuilder stringBuilder, int indentLevel, boolean indentFirstLine) {
        if (indentFirstLine) { stringBuilder.append("  ".repeat(indentLevel)); }
        stringBuilder.append("FunctionDefinitionNode(\n");
        stringBuilder.append("  ".repeat(indentLevel + 1));
        stringBuilder.append("name=\"").append(id.id).append("\",\n");
        stringBuilder.append("  ".repeat(indentLevel + 1)).append("type=");
        funcType.genFormattedString(stringBuilder);
        stringBuilder.append(",\n");
        stringBuilder.append("  ".repeat(indentLevel + 1)).append("storage=");
        if (storageClass != null) {
            storageClass.genFormattedString(stringBuilder, indentLevel + 1, false);
            stringBuilder.append("\n");
        } else {
            stringBuilder.append("null\n");
        }
        stringBuilder.append("  ".repeat(indentLevel + 1)).append("body=");
        body.genFormattedString(stringBuilder, indentLevel + 1, false);
        stringBuilder.append("  ".repeat(indentLevel)).append(")\n");
    }
}
