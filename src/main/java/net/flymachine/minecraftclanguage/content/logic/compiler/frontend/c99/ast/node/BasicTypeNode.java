package net.flymachine.minecraftclanguage.content.logic.compiler.frontend.c99.ast.node;

import net.flymachine.minecraftclanguage.content.logic.compiler.common.type.BasicType;
import net.flymachine.minecraftclanguage.content.logic.compiler.frontend.c99.ast.AstVisitor;
import net.flymachine.minecraftclanguage.content.logic.errorHandle.SourceLocation;

public final class BasicTypeNode extends TypeNode {
    public BasicType.Primitive primitive;

    public BasicTypeNode(SourceLocation wholeLocation, BasicType.Primitive primitive) {
        super(wholeLocation);
        this.primitive = primitive;
    }

    @Override
    public <T> T accept(AstVisitor<T> visitor) {
        return null;
    }

    @Override
    public BasicType getType() {
        return new BasicType(primitive, constQualifier != null);
    }

    @Override
    protected String format(String declarator) {
        return getType().format(declarator);
    }
}
