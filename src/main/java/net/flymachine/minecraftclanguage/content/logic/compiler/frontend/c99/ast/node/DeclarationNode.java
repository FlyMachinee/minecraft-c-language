package net.flymachine.minecraftclanguage.content.logic.compiler.frontend.c99.ast.node;

import net.flymachine.minecraftclanguage.content.logic.compiler.frontend.c99.ast.AstVisitor;
import net.flymachine.minecraftclanguage.content.logic.errorHandle.SourceLocation;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.PrintStream;
import java.util.List;

public final class DeclarationNode extends AstNode implements ExternalDeclarationNode {
    public @Nullable StorageClassSpecifierNode storageClass;
    public TypeNode baseType;
    public final @NotNull List<InitDeclaratorNode> initDeclarators;

    public DeclarationNode(
        SourceLocation wholeLocation, @Nullable StorageClassSpecifierNode storageClass, TypeNode baseType,
        @NotNull List<InitDeclaratorNode> initDeclarators) {

        super(wholeLocation);
        this.storageClass = storageClass;
        this.baseType = baseType;
        this.initDeclarators = initDeclarators;
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
        stream.print("baseType=");
        baseType.dump(stream);
        stream.println(',');

        indent(stream, indentLevel + 1);
        if (initDeclarators.isEmpty()) {
            stream.println("list=[]");
        } else {
            stream.print("list=[");
            for (InitDeclaratorNode initDeclarator : initDeclarators) {
                stream.println("{");

                indent(stream, indentLevel + 2);
                stream.print("id=");
                initDeclarator.id.dump(stream);
                stream.println(",");

                indent(stream, indentLevel + 2);
                stream.print("finalType=");
                initDeclarator.finalType.dump(stream);
                stream.println(",");

                if (initDeclarator.init != null) {
                    indent(stream, indentLevel + 2);
                    stream.print("init=");
                    initDeclarator.init.dump(stream, indentLevel + 2, false);
                    stream.println(",");
                }

                indent(stream, indentLevel + 1);
                stream.print("}, ");
            }
            stream.println("],");
        }

        indent(stream, indentLevel);
        stream.print(')');
    }
}
