package net.flymachine.minecraftclanguage.content.logic.compiler.frontend.c99.ast.node;

import net.flymachine.minecraftclanguage.content.logic.compiler.common.type.Type;
import net.flymachine.minecraftclanguage.content.logic.compiler.common.type.TypedefType;
import net.flymachine.minecraftclanguage.content.logic.compiler.frontend.c99.ast.AstVisitor;
import net.flymachine.minecraftclanguage.content.logic.errorHandle.SourceLocation;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class TypedefNameNode extends TypeNode {

    public final @NotNull IdentifierNode id;
    private @Nullable TypeNode underlyingType;

    public TypedefNameNode(SourceLocation wholeLocation, @NotNull IdentifierNode id) {
        super(wholeLocation);
        this.id = id;
    }

    public void resolve(TypeNode underlyingType) {
        if (this.underlyingType != null) {
            throw new IllegalStateException("TypedefNameNode is already resolved");
        }
        this.underlyingType = underlyingType.unwrap();
    }

    public boolean resolved() {
        return underlyingType != null;
    }

    public @NotNull TypeNode underlyingType() {
        if (underlyingType == null) {
            throw new IllegalStateException("TypedefNameNode is not resolved yet");
        }
        return underlyingType;
    }

    @Override
    public Type getType() {
        return new TypedefType(id.name, underlyingType().getType(), constQualifier != null);
    }

    @Override
    public boolean containsAlias() {
        return true;
    }

    @Override
    public TypeNode unwrap() {
        TypeNode newType = underlyingType().unwrap();
        if (constQualifier != null) {
            newType.constQualifier = constQualifier;
        }
        return newType;
    }

    @Override
    protected String format(String declarator) {
        String name = constQualifier != null ? "const " + id.name : id.name;
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
