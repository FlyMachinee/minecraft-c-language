package net.flymachine.minecraftclanguage.content.logic.compiler.common.type;

import net.flymachine.minecraftclanguage.content.logic.compiler.backend.la64.highLevel.AsmType;

public enum BasicType implements Type {
    VOID,
    INT,
    LONG;

    @Override
    public boolean isCompatible(Type other) {
        if (!(other instanceof BasicType)) {
            return false;
        }
        return this == other;
    }

    @Override
    public boolean isComplete() {
        return this != VOID;
    }

    @Override
    public boolean isArithmetic() {
        return this != VOID;
    }

    @Override
    public boolean isScalar() {
        return this != VOID;
    }

    @Override
    public boolean isInteger() {
        return this != VOID;
    }

    @Override
    public boolean isReal() {
        return this != VOID;
    }

    @Override
    public long sizeof() {
        return switch (this) {
            case VOID -> throw new UnsupportedOperationException("sizeof(void) is not defined");
            case INT -> 4;
            case LONG -> 8;
        };
    }

    @Override
    public AsmType toAsmType() {
        return switch (this) {
            case VOID -> throw new UnsupportedOperationException("toAsmType(void) is not defined");
            case INT -> AsmType.WORD;
            case LONG -> AsmType.DWORD;
        };
    }

    @Override
    public <R> R accept(TypeVisitor<R> visitor) {
        return visitor.visit(this);
    }

    @Override
    public String toString() {
        return name().toLowerCase();
    }

    public static BasicType fromString(String typeName) {
        return valueOf(typeName.toUpperCase());
    }
}
