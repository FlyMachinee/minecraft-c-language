package net.flymachine.minecraftclanguage.content.logic.compiler.frontend.c99.ast.node;

import net.flymachine.minecraftclanguage.content.logic.compiler.common.type.*;

public interface TypeNode extends AstInterface {
    Type getType();

    static TypeNode fromType(Type type) {
        if (type instanceof ErrorType) {
            throw new IllegalArgumentException("Cannot create TypeNode from ErrorType");
        }
        if (type instanceof BasicType bt) {
            return new BasicTypeNode(null, bt);
        }
        if (type instanceof FunctionType ft) {
            return new FunctionTypeNode(
                null, fromType(ft.returnType()),
                ft.parameterTypes().stream().map(TypeNode::fromType).toList(), null);
        }
        if (type instanceof PointerType pt) {
            return new PointerTypeNode(null, fromType(pt.referencedType()));
        }
        throw new IllegalArgumentException("Unknown Type " + type);
    }
}
