package net.flymachine.minecraftclanguage.content.logic.compiler.frontend.c99.ast.node.type;

import net.flymachine.minecraftclanguage.content.logic.compiler.frontend.c99.ast.node.IdentifierNode;
import net.flymachine.minecraftclanguage.content.logic.compiler.frontend.c99.enumerate.TagKind;
import net.flymachine.minecraftclanguage.content.logic.errorHandle.SourceLocation;

import javax.annotation.Nullable;

public abstract sealed class TaggedTypeNode extends TypeNode permits EnumTypeNode, RecordTypeNode {

    public final @Nullable IdentifierNode tag;

    protected TaggedTypeNode(SourceLocation wholeLocation, @Nullable IdentifierNode tag) {
        super(wholeLocation);
        this.tag = tag;
    }

    public abstract boolean resolved();

    public abstract TagKind tagKind();
}
