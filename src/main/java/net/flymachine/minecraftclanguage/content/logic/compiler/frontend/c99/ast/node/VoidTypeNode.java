package net.flymachine.minecraftclanguage.content.logic.compiler.frontend.c99.ast.node;

import net.flymachine.minecraftclanguage.content.logic.compiler.common.type.VoidType;
import net.flymachine.minecraftclanguage.content.logic.compiler.frontend.c99.ast.AstVisitor;
import net.flymachine.minecraftclanguage.content.logic.errorHandle.SourceLocation;

public final class VoidTypeNode extends TypeNode {

    public VoidTypeNode(SourceLocation wholeLoc) {
        super(wholeLoc);
    }

    @Override
    public VoidType getType() {
        return VoidType.INSTANCE;
    }

    @Override
    public String format(String declarator) {
        return getType().format(declarator);
    }

    @Override
    public <T> T accept(AstVisitor<T> visitor) {
        return null;
    }
}
