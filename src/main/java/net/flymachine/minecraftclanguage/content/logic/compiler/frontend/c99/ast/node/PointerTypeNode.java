package net.flymachine.minecraftclanguage.content.logic.compiler.frontend.c99.ast.node;

import net.flymachine.minecraftclanguage.content.logic.compiler.common.type.PointerType;
import net.flymachine.minecraftclanguage.content.logic.compiler.frontend.c99.ast.AstVisitor;
import net.flymachine.minecraftclanguage.content.logic.errorHandle.SourceLocation;

import java.io.PrintStream;

public final class PointerTypeNode extends TypeNode {
    public TypeNode referencedType;

    public PointerTypeNode(SourceLocation wholeLocation, TypeNode referencedType) {
        super(wholeLocation);
        this.referencedType = referencedType;
    }

    @Override
    public PointerType getType() {
        return new PointerType(referencedType.getType(), constQualifier != null);
    }

    @Override
    protected String format(String declarator) {
        String newDeclarator = "*" + (isConst() ? " const" : "") + declarator;
        return referencedType.format(newDeclarator);
    }

    @Override
    public <T> T accept(AstVisitor<T> visitor) {
        return null;
    }

    @Override
    public void dump(PrintStream stream, int indentLevel, boolean indentFirstLine) {
        stream.print(typename());
    }
}
