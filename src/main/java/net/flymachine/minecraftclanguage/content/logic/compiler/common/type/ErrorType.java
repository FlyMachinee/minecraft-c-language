package net.flymachine.minecraftclanguage.content.logic.compiler.common.type;

import net.flymachine.minecraftclanguage.content.logic.compiler.backend.la64.highLevel.AsmType;

public final class ErrorType extends Type {

    @Override
    TypeKind kind() {
        return TypeKind.ERROR;
    }

    @Override
    public String format(String declarator) {
        return "<error-type>";
    }

    private ErrorType(boolean isConst) {
        super(isConst);
    }

    public static final ErrorType INSTANCE = new ErrorType(false);

    @Override
    public ErrorType setConst(boolean isConst) {
        return INSTANCE;
    }

    @Override
    public boolean isConst() {
        return false;
    }

    @Override
    public boolean isCompatible(Type other) {
        return false;
    }

    @Override
    public Type merge(Type other) {
        return INSTANCE;
    }

    @Override
    public boolean isComplete() {
        return false;
    }

    @Override
    public long sizeof() {
        throw new UnsupportedOperationException("sizeof(error) is not defined");
    }

    @Override
    public long alignof() {
        throw new UnsupportedOperationException("alignof(error) is not defined");
    }

    @Override
    public AsmType toAsmType() {
        throw new UnsupportedOperationException("toAsmType(error) is not defined");
    }
}
