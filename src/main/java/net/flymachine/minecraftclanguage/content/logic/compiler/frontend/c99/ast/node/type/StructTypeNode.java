package net.flymachine.minecraftclanguage.content.logic.compiler.frontend.c99.ast.node.type;

import net.flymachine.minecraftclanguage.content.logic.compiler.common.type.StructType;
import net.flymachine.minecraftclanguage.content.logic.compiler.common.type.record.RecordInfo;
import net.flymachine.minecraftclanguage.content.logic.compiler.common.type.record.StructInfo;
import net.flymachine.minecraftclanguage.content.logic.compiler.frontend.c99.ast.AstVisitor;
import net.flymachine.minecraftclanguage.content.logic.compiler.frontend.c99.ast.node.IdentifierNode;
import net.flymachine.minecraftclanguage.content.logic.compiler.frontend.c99.ast.node.type.record.MemberDeclarationNode;
import net.flymachine.minecraftclanguage.content.logic.compiler.frontend.c99.enumerate.TagKind;
import net.flymachine.minecraftclanguage.content.logic.errorHandle.SourceLocation;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public final class StructTypeNode extends RecordTypeNode<StructInfo> {

    public StructTypeNode(SourceLocation wholeLocation, @Nullable IdentifierNode tag,
        @Nullable List<MemberDeclarationNode> memberDeclarations) {
        super(wholeLocation, tag, memberDeclarations);
    }

    @Override
    protected StructInfo narrowInfo(RecordInfo info) {
        return (StructInfo) info;
    }

    @Override
    public StructType getType() {
        if (!resolved()) {
            throw new IllegalStateException("StructTypeNode is not resolved yet");
        }
        return new StructType(getInfo(), constQualifier != null);
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

    @Override
    public TagKind tagKind() {
        return TagKind.STRUCT;
    }
}
