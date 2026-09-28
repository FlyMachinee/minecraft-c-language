package net.flymachine.minecraftclanguage.content.logic.compiler.common.type;

import net.flymachine.minecraftclanguage.content.logic.compiler.backend.la64.highLevel.AsmType;
import org.jetbrains.annotations.NotNull;

public final class VoidType extends Type {
    public static final VoidType INSTANCE = new VoidType(false);
    private static final VoidType CONST_VOID = new VoidType(true);

    @Override
    TypeKind kind() {
        return TypeKind.VOID;
    }

    @Override
    public String format(String declarator) {
        if (declarator.isEmpty()) {
            return "void";
        }
        if (declarator.startsWith("[")) {
            return "void" + declarator;
        }
        return "void " + declarator;
    }

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
    public Type merge(Type other) {
        if (!this.isCompatible(other)) {
            return ErrorType.INSTANCE;
        }
        return this;
    }

    @Override
    public boolean isComplete() {
        return false;
    }

    @Override
    public long sizeof() {
        throw new UnsupportedOperationException("sizeof(void) is not defined");
    }

    @Override
    public long alignof() {
        throw new UnsupportedOperationException("alignof(void) is not defined");
    }

    @Override
    public AsmType toAsmType() {
        throw new UnsupportedOperationException("toAsmType(void) is not defined");
    }

    @Override
    public @NotNull String toString() {
        return "void";
    }
}
