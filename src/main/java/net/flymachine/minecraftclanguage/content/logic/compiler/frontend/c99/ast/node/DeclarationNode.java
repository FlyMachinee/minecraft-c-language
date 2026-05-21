package net.flymachine.minecraftclanguage.content.logic.compiler.frontend.c99.ast.node;

import net.flymachine.minecraftclanguage.content.logic.compiler.frontend.c99.ast.AstVisitor;
import net.flymachine.minecraftclanguage.content.logic.errorHandle.SourceLocation;
import org.jetbrains.annotations.Nullable;

import java.io.PrintStream;

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
    public void dump(PrintStream stream, int indentLevel, boolean indentFirstLine) {
        if (indentFirstLine) {
            indent(stream, indentLevel);
        }

        stream.println("DeclarationNode(");

        indent(stream, indentLevel + 1);
        stream.print("storageClass=");
        if (storageClass != null) {
            storageClass.dump(stream);
            stream.println(',');
        } else {
            stream.println("null,");
        }

        indent(stream, indentLevel + 1);
        stream.print("type=");
        type.dump(stream);
        stream.println(',');

        indent(stream, indentLevel + 1);
        stream.print("id=");
        id.dump(stream);
        stream.println(',');

        indent(stream, indentLevel + 1);
        stream.print("init=");
        if (init != null) {
            init.dump(stream, indentLevel + 1, false);
            stream.println(',');
        } else {
            stream.println("null,");
        }

        indent(stream, indentLevel);
        stream.print(')');
    }
}
