package net.flymachine.minecraftclanguage.content.logic.compiler.frontend.c99.ast.node;

import net.flymachine.minecraftclanguage.content.logic.compiler.common.type.EnumInfo;
import net.flymachine.minecraftclanguage.content.logic.compiler.common.type.EnumType;
import net.flymachine.minecraftclanguage.content.logic.compiler.frontend.c99.ast.AstVisitor;
import net.flymachine.minecraftclanguage.content.logic.errorHandle.SourceLocation;

import javax.annotation.Nullable;
import java.util.List;

public final class EnumTypeNode extends TaggedTypeNode {
    public final @Nullable List<EnumeratorNode> enumerators;
    private @Nullable EnumInfo info;

    public EnumTypeNode(SourceLocation wholeLocation, @Nullable IdentifierNode tag,
        @Nullable List<EnumeratorNode> enumerators) {
        super(wholeLocation, tag);
        this.enumerators = enumerators;
    }

    @Override
    public boolean resolved() { return info != null; }

    public void resolve(EnumInfo info) {
        if (this.info != null) { throw new IllegalStateException("EnumTypeNode is already resolved"); }
        this.info = info;
    }

    public EnumInfo getInfo() {
        if (info == null) { throw new IllegalStateException("EnumTypeNode is not resolved yet"); }
        return info;
    }

    @Override
    public EnumType getType() {
        return new EnumType(getInfo(), constQualifier != null);
    }

    @Override
    protected String format(String declarator) {
        String name = constQualifier != null ? "const enum " : "enum ";
        name += tag != null ? tag.name : "<anonymous>";
        if (declarator.isEmpty()) { return name; }
        if (declarator.startsWith("[")) { return name + declarator; }
        return name + " " + declarator;
    }

    @Override
    public <T> T accept(AstVisitor<T> visitor) {
        return null;
    }
}
