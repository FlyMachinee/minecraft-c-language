package net.flymachine.minecraftclanguage.content.logic.compiler.frontend.c99.ast.node;

import net.flymachine.minecraftclanguage.content.logic.compiler.common.type.BasicType;
import net.flymachine.minecraftclanguage.content.logic.compiler.frontend.c99.ast.AstVisitor;
import net.flymachine.minecraftclanguage.content.logic.errorHandle.SourceLocation;

public final class BasicTypeNode extends AstNode implements TypeNode {
    public BasicType type;

    public BasicTypeNode(SourceLocation wholeLocation, BasicType type) {
        super(wholeLocation);
        this.type = type;
    }

    @Override
    public <T> T accept(AstVisitor<T> visitor) {
        return null;
    }

    @Override
    public void genFormattedString(StringBuilder stringBuilder, int indentLevel, boolean indentFirstLine) {
        stringBuilder.append(type);
    }

    @Override
    public BasicType getType() {
        return type;
    }
}
