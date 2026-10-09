package net.flymachine.minecraftclanguage.content.logic.compiler.frontend.c99.ast.node;

import net.flymachine.minecraftclanguage.content.logic.compiler.common.type.StructInfo;
import net.flymachine.minecraftclanguage.content.logic.compiler.common.type.StructType;
import net.flymachine.minecraftclanguage.content.logic.compiler.common.type.Type;
import net.flymachine.minecraftclanguage.content.logic.compiler.frontend.c99.ast.AstVisitor;
import net.flymachine.minecraftclanguage.content.logic.errorHandle.SourceLocation;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public final class StructTypeNode extends TypeNode {
    public final @Nullable IdentifierNode tag;
    public final @Nullable List<MemberDeclarationNode> memberDeclarations;

    private StructType resolvedType;

    public StructTypeNode(SourceLocation wholeLocation, @Nullable IdentifierNode tag,
        @Nullable List<MemberDeclarationNode> memberDeclarations) {

        super(wholeLocation);
        this.tag = tag;
        this.memberDeclarations = memberDeclarations;
    }

    public void resolve(StructInfo info) {
        if (this.resolvedType != null) {
            throw new IllegalStateException("StructTypeNode is already resolved");
        }
        this.resolvedType = new StructType(info, constQualifier != null);
    }

    public boolean resolved() {
        return resolvedType != null;
    }

    @Override
    public Type getType() {
        if (resolvedType == null) {
            throw new IllegalStateException("StructTypeNode is not resolved yet");
        }
        return resolvedType;
    }

    @Override
    protected String format(String declarator) {
        String name = constQualifier != null ? "const struct " : "struct ";
        if (tag != null) {
            name += tag.name;
        } else {
            name += " <anonymous>";
        }

        if (declarator.isEmpty()) {
            return name;
        }
        if (declarator.startsWith("[")) {
            return name + declarator;
        }
        return name + " " + declarator;

    }

    @Override
    public <T> T accept(AstVisitor<T> visitor) {
        return null;
    }
}
