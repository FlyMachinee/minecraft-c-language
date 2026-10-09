package net.flymachine.minecraftclanguage.content.logic.compiler.frontend.c99.ast.node;

import net.flymachine.minecraftclanguage.content.logic.compiler.frontend.c99.ast.AstVisitor;
import net.flymachine.minecraftclanguage.content.logic.errorHandle.SourceLocation;
import org.jetbrains.annotations.NotNull;

import java.io.PrintStream;
import java.util.List;

public final class MemberDeclarationNode extends AstNode {
    public final TypeNode baseType;
    public final @NotNull List<MemberDeclaratorNode> memberDeclarators;

    public MemberDeclarationNode(
        SourceLocation wholeLocation, TypeNode baseType, @NotNull List<MemberDeclaratorNode> memberDeclarators) {
        super(wholeLocation);
        this.baseType = baseType;
        this.memberDeclarators = memberDeclarators;
    }

    @Override
    public <T> T accept(AstVisitor<T> visitor) {
        return null;
    }

    @Override
    public void dump(PrintStream stream, int indentLevel, boolean indentFirstLine) {
        if (indentFirstLine) {
            indent(stream, indentLevel);
        }

        stream.println("MemberDeclarationNode(");

        indent(stream, indentLevel + 1);
        stream.print("baseType=");
        baseType.dump(stream);
        stream.println(',');

        indent(stream, indentLevel + 1);
        if (memberDeclarators.isEmpty()) {
            stream.println("list=[]");
        } else {
            stream.println("list=[");
            for (MemberDeclaratorNode structDeclarator : memberDeclarators) {
                indent(stream, indentLevel + 2);
                stream.print("{ id=");
                structDeclarator.id.dump(stream);
                stream.print(", finalType=");
                structDeclarator.finalType.dump(stream);
                stream.println(" },");
            }
            indent(stream, indentLevel + 1);
            stream.println("],");
        }

        indent(stream, indentLevel);
        stream.print(')');
    }
}
