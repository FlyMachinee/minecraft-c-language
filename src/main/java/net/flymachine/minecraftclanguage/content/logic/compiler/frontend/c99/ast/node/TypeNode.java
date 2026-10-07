package net.flymachine.minecraftclanguage.content.logic.compiler.frontend.c99.ast.node;

import net.flymachine.minecraftclanguage.content.logic.compiler.common.type.*;
import net.flymachine.minecraftclanguage.content.logic.errorHandle.SourceLocation;

import java.io.PrintStream;

public abstract class TypeNode extends AstNode {
    public ConstQualifierNode constQualifier = null;

    protected TypeNode(SourceLocation wholeLoc) {
        super(wholeLoc);
    }

    public abstract Type getType();

    public static TypeNode fromType(Type type) {
        if (type instanceof ErrorType) {
            throw new IllegalArgumentException("Cannot create TypeNode from ErrorType");
        }
        if (type instanceof BasicType bt) {
            BasicTypeNode node = new BasicTypeNode(null, bt.primitive());
            if (bt.isConst()) {
                node.constQualifier = new ConstQualifierNode(null);
            }
            return node;
        }
        if (type instanceof FunctionType ft) {
            return new FunctionTypeNode(
                null, fromType(ft.returnType()),
                ft.parameterTypes().stream().map(TypeNode::fromType).toList(), null);
        }
        if (type instanceof PointerType pt) {
            PointerTypeNode node = new PointerTypeNode(null, fromType(pt.referencedType()));
            if (pt.isConst()) {
                node.constQualifier = new ConstQualifierNode(null);
            }
            return node;
        }
        if (type instanceof ArrayType at) {
            return new ArrayTypeNode(null, fromType(at.elementType()), new ConstantNode(null, at.size()));
        }
        throw new IllegalArgumentException("Unknown Type " + type);
    }

    public boolean isConst() {
        return constQualifier != null;
    }

    protected abstract String format(String declarator);

    public String typename() {
        return format("");
    }

    @Override
    public void dump(PrintStream stream, int indentLevel, boolean indentFirstLine) {
        stream.print(typename());
    }
}
