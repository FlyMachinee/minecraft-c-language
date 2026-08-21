package net.flymachine.minecraftclanguage.content.logic.compiler.common.type;

public interface TypeVisitor<R> {
    R visit(VoidType t);

    R visit(BasicType t);

    R visit(FunctionType t);

    R visit(PointerType t);
}
