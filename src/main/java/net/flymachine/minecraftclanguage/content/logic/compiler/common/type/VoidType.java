package net.flymachine.minecraftclanguage.content.logic.compiler.common.type;

import net.flymachine.minecraftclanguage.content.logic.compiler.backend.la64.highLevel.AsmType;

public final class VoidType extends Type {
    public static final VoidType INSTANCE = new VoidType(false);
    private static final VoidType CONST_VOID = new VoidType(true);

    private VoidType(boolean isConst) {
        super(isConst);
    }

    @Override
    public VoidType setConst(boolean isConst) {
        return isConst ? CONST_VOID : INSTANCE;
    }

    @Override
    public boolean isCompatible(Type other) {
        return other.isVoid() && (isConst == other.isConst);
    }

    @Override
    public boolean isComplete() {
        return false;
    }

    @Override
    public boolean isArithmetic() {
        return false;
    }

    @Override
    public boolean isScalar() {
        return false;
    }

    @Override
    public boolean isInteger() {
        return false;
    }

    @Override
    public boolean isReal() {
        return false;
    }

    @Override
    public long sizeof() {
        throw new UnsupportedOperationException("void size is unknown");
    }

    @Override
    public AsmType toAsmType() {
        throw new UnsupportedOperationException("toAsmType(void) is not defined");
    }

    @Override
    public <R> R accept(TypeVisitor<R> visitor) {
        return visitor.visit(this);
    }
}
