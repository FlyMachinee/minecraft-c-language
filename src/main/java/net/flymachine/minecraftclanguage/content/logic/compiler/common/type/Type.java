package net.flymachine.minecraftclanguage.content.logic.compiler.common.type;

public interface Type {
    boolean isCompatible(Type other);

    boolean isAssignableFrom(Type other);

    boolean isComplete();

    <R> R accept(TypeVisitor<R> visitor);
}
