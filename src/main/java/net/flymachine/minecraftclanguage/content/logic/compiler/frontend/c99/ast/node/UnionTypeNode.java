package net.flymachine.minecraftclanguage.content.logic.compiler.frontend.c99.ast.node;

import net.flymachine.minecraftclanguage.content.logic.compiler.common.type.UnionType;
import net.flymachine.minecraftclanguage.content.logic.compiler.common.type.record.RecordInfo;
import net.flymachine.minecraftclanguage.content.logic.compiler.common.type.record.UnionInfo;
import net.flymachine.minecraftclanguage.content.logic.compiler.frontend.c99.ast.AstVisitor;
import net.flymachine.minecraftclanguage.content.logic.errorHandle.SourceLocation;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public final class UnionTypeNode extends RecordTypeNode<UnionInfo> {

    public UnionTypeNode(SourceLocation wholeLocation, @Nullable IdentifierNode tag,
        @Nullable List<MemberDeclarationNode> memberDeclarations) {
        super(wholeLocation, tag, memberDeclarations);
    }

    @Override
    protected UnionInfo narrowInfo(RecordInfo info) {
        return (UnionInfo) info;
    }

    @Override
    public UnionType getType() {
        if (!resolved()) {
            throw new IllegalStateException("UnionTypeNode is not resolved yet");
        }
        return new UnionType(getInfo(), constQualifier != null);
    }

    @Override
    protected String format(String declarator) {
        String name = constQualifier != null ? "const union " : "union ";
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
