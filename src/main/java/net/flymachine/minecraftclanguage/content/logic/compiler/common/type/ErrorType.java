package net.flymachine.minecraftclanguage.content.logic.compiler.common.type;

import net.flymachine.minecraftclanguage.content.logic.compiler.backend.la64.highLevel.AsmType;

public final class ErrorType implements Type {

    private ErrorType() { }

    public static final ErrorType INSTANCE = new ErrorType();

    @Override
    public boolean isCompatible(Type other) {
        return true;
    }

    @Override
    public boolean isComplete() {
        return true;
    }

    @Override
    public boolean isArithmetic() {
        return true;
    }

    @Override
    public boolean isScalar() {
        return true;
    }

    @Override
    public boolean isInteger() {
        return true;
    }

    @Override
    public boolean isReal() {
        return true;
    }

    @Override
    public long sizeof() {
        throw new UnsupportedOperationException("sizeof(error) is not defined");
    }

    @Override
    public AsmType toAsmType() {
        throw new UnsupportedOperationException("toAsmType(error) is not defined");
    }

    @Override
    public <R> R accept(TypeVisitor<R> visitor) {
        return null;
    }
}
