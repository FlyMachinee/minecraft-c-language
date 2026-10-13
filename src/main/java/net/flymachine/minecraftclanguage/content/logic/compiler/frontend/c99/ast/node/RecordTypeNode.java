package net.flymachine.minecraftclanguage.content.logic.compiler.frontend.c99.ast.node;

import net.flymachine.minecraftclanguage.content.logic.compiler.common.type.RecordType;
import net.flymachine.minecraftclanguage.content.logic.compiler.common.type.record.RecordInfo;
import net.flymachine.minecraftclanguage.content.logic.errorHandle.SourceLocation;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public abstract sealed class RecordTypeNode<I extends RecordInfo> extends TypeNode permits StructTypeNode, UnionTypeNode {
    public final @Nullable IdentifierNode tag;
    public final @Nullable List<MemberDeclarationNode> memberDeclarations;
    protected @Nullable RecordInfo info;

    protected RecordTypeNode(SourceLocation wholeLoc, @Nullable IdentifierNode tag,
        @Nullable List<MemberDeclarationNode> memberDeclarations) {
        super(wholeLoc);
        this.tag = tag;
        this.memberDeclarations = memberDeclarations;
    }

    public final void resolve(RecordInfo info) {
        if (this.info != null) {
            throw new IllegalStateException("RecordTypeNode is already resolved");
        }
        this.info = info;
    }

    public final boolean resolved() {
        return info != null;
    }

    protected abstract I narrowInfo(RecordInfo info);

    public final I getInfo() {
        if (!resolved()) {
            throw new IllegalStateException("RecordTypeNode is not resolved yet");
        }
        return narrowInfo(info);
    }

    @Override
    public abstract RecordType<I> getType();
}
