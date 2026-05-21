package net.flymachine.minecraftclanguage.content.logic.compiler.frontend.c99.ast.node;

import net.flymachine.minecraftclanguage.content.logic.compiler.frontend.c99.ast.AstVisitor;
import net.flymachine.minecraftclanguage.content.logic.errorHandle.SourceLocation;
import org.jetbrains.annotations.Nullable;

import java.io.PrintStream;

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
    public void dump(PrintStream stream, int indentLevel, boolean indentFirstLine) {
        if (indentFirstLine) {
            indent(stream, indentLevel);
        }

        stream.println("FunctionDefinitionNode(");

        indent(stream, indentLevel + 1);
        stream.print("name=");
        id.dump(stream);
        stream.println(',');

        indent(stream, indentLevel + 1);
        stream.print("type=");
        funcType.dump(stream);
        stream.println(',');

        indent(stream, indentLevel + 1);
        stream.print("storageClass=");
        if (storageClass != null) {
            storageClass.dump(stream);
            stream.println(',');
        } else {
            stream.println("null,");
        }

        indent(stream, indentLevel + 1);
        stream.print("body=");
        body.dump(stream, indentLevel + 1, false);
        stream.println(',');

        indent(stream, indentLevel);
        stream.print(')');
    }
}
