package net.flymachine.minecraftclanguage.content.logic.compiler.frontend.c99.ast.node;

import net.flymachine.minecraftclanguage.content.logic.compiler.common.type.BasicType;
import net.flymachine.minecraftclanguage.content.logic.compiler.frontend.c99.ast.AstVisitor;
import net.flymachine.minecraftclanguage.content.logic.errorHandle.SourceLocation;

import java.io.PrintStream;

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
    public void dump(PrintStream stream, int indentLevel, boolean indentFirstLine) {
        stream.print(type);
    }

    @Override
    public BasicType getType() {
        return type;
    }
}
