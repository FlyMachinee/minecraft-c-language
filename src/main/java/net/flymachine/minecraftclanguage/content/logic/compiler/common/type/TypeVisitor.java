package net.flymachine.minecraftclanguage.content.logic.compiler.common.type;

public interface TypeVisitor<R> {
    R visit(BasicType t);

    R visit(FunctionType t);
}
