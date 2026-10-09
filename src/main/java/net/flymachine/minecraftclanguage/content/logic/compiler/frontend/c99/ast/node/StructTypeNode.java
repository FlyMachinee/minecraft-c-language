package net.flymachine.minecraftclanguage.content.logic.compiler.frontend.c99.ast.node;

import net.flymachine.minecraftclanguage.content.logic.compiler.common.type.Type;
import net.flymachine.minecraftclanguage.content.logic.compiler.frontend.c99.ast.AstVisitor;
import net.flymachine.minecraftclanguage.content.logic.errorHandle.SourceLocation;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public final class StructTypeNode extends TypeNode {
    public final @Nullable IdentifierNode tag;
    public final @Nullable List<MemberDeclarationNode> memberDeclarations;


    public StructTypeNode(SourceLocation wholeLocation, @Nullable IdentifierNode tag,
        @Nullable List<MemberDeclarationNode> memberDeclarations) {

        super(wholeLocation);
        this.tag = tag;
        this.memberDeclarations = memberDeclarations;
    }

    @Override
    public Type getType() {
        return null;
    }

    @Override
    protected String format(String declarator) {
        return "";
    }

    @Override
    public <T> T accept(AstVisitor<T> visitor) {
        return null;
    }
}
