package net.flymachine.minecraftclanguage.content.logic.compiler.common.type;

import net.flymachine.minecraftclanguage.content.logic.compiler.backend.la64.highLevel.AsmType;

public enum BasicType implements Type {
    VOID("void"),
    INT("int"),
    LONG("long"),
    UNSIGNED_INT("unsigned int"),
    UNSIGNED_LONG("unsigned long");

    private final String name;

    BasicType(String name) {
        this.name = name;
    }

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
            case VOID -> -1;
            case INT, UNSIGNED_INT -> 4;
            case LONG, UNSIGNED_LONG -> 8;
        };
    }

    @Override
    public AsmType toAsmType() {
        return switch (this) {
            case VOID -> throw new UnsupportedOperationException("toAsmType(void) is not defined");
            case INT, UNSIGNED_INT -> AsmType.WORD;
            case LONG, UNSIGNED_LONG -> AsmType.DWORD;
        };
    }

    @Override
    public <R> R accept(TypeVisitor<R> visitor) {
        return visitor.visit(this);
    }

    @Override
    public String toString() {
        return name;
    }

    public static BasicType fromString(String typeName) {
        for (BasicType type : BasicType.values()) {
            if (type.name.equals(typeName)) {
                return type;
            }
        }
        return null;
    }

    public boolean isSigned() {
        return this == INT || this == LONG;
    }

    public boolean isUnsigned() {
        return this == UNSIGNED_INT || this == UNSIGNED_LONG;
    }
}
