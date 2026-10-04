package net.flymachine.minecraftclanguage.content.logic.compiler.common.type;

import net.flymachine.minecraftclanguage.content.logic.compiler.backend.la64.highLevel.AsmType;
import org.jetbrains.annotations.NotNull;

public final class BasicType extends Type {
    public enum Primitive {
        INT("int"),
        LONG("long"),
        UNSIGNED_INT("unsigned int"),
        UNSIGNED_LONG("unsigned long"),
        CHAR("char"),
        SIGNED_CHAR("signed char"),
        UNSIGNED_CHAR("unsigned char"),
        DOUBLE("double");

        private final String name;

        Primitive(String name) {
            this.name = name;
        }
    }

    public static final BasicType INT = new BasicType(Primitive.INT);
    public static final BasicType LONG = new BasicType(Primitive.LONG);
    public static final BasicType DOUBLE = new BasicType(Primitive.DOUBLE);
    public static final BasicType UNSIGNED_INT = new BasicType(Primitive.UNSIGNED_INT);
    public static final BasicType UNSIGNED_LONG = new BasicType(Primitive.UNSIGNED_LONG);
    public static final BasicType CHAR = new BasicType(Primitive.CHAR);
    public static final BasicType SIGNED_CHAR = new BasicType(Primitive.SIGNED_CHAR);
    public static final BasicType UNSIGNED_CHAR = new BasicType(Primitive.UNSIGNED_CHAR);

    private final @NotNull Primitive primitive;

    public BasicType(@NotNull Primitive primitive) {
        super(false);
        this.primitive = primitive;
    }

    public BasicType(@NotNull Primitive primitive, boolean isConst) {
        super(isConst);
        this.primitive = primitive;
    }

    public Primitive primitive() {
        return primitive;
    }

    @Override
    TypeKind kind() {
        return TypeKind.BASIC;
    }

    @Override
    public String format(String declarator) {
        if (declarator.isEmpty()) {
            return toString();
        }
        if (declarator.startsWith("[")) {
            return this + declarator;
        }
        return this + " " + declarator;
    }

    @Override
    public BasicType setConst(boolean isConst) {
        if (this.isConst == isConst) {
            return this;
        } else {
            return new BasicType(primitive, isConst);
        }
    }

    @Override
    public boolean isCompatible(Type other) {
        if (this.isConst() != other.isConst()) {
            return false;
        }
        if (!(other instanceof BasicType o)) {
            return false;
        }
        return this.primitive == o.primitive;
    }

    @Override
    public Type merge(Type other) {
        if (this.isCompatible(other)) {
            return this;
        }
        return ErrorType.INSTANCE;
    }

    @Override
    public boolean isComplete() {
        return true;
    }

    @Override
    public long sizeof() {
        return switch (this.primitive) {
            case CHAR, SIGNED_CHAR, UNSIGNED_CHAR -> 1;
            case INT, UNSIGNED_INT -> 4;
            case LONG, UNSIGNED_LONG, DOUBLE -> 8;
        };
    }

    @Override
    public long alignof() {
        return sizeof();
    }

    @Override
    public AsmType toAsmType() {
        return switch (this.primitive) {
            case CHAR, SIGNED_CHAR, UNSIGNED_CHAR ->
                throw new UnsupportedOperationException("char type is not supported yet in assembly");
            case INT, UNSIGNED_INT -> AsmType.WORD;
            case LONG, UNSIGNED_LONG -> AsmType.DWORD;
            case DOUBLE -> AsmType.DOUBLE;
        };
    }

    @Override
    public String toString() {
        String res = primitive.name;
        if (isConst) {
            res = "const " + res;
        }
        return res;
    }

    public static BasicType fromString(String typeName) {
        return switch (typeName) {
            case "int" -> INT;
            case "long" -> LONG;
            case "double" -> DOUBLE;
            case "unsigned int" -> UNSIGNED_INT;
            case "unsigned long" -> UNSIGNED_LONG;
            case "char" -> CHAR;
            case "signed char" -> SIGNED_CHAR;
            case "unsigned char" -> UNSIGNED_CHAR;
            default -> throw new IllegalArgumentException("Unknown type: " + typeName);
        };
    }

    public boolean isSigned() {
        return switch (this.primitive) {
            case INT, LONG, CHAR, SIGNED_CHAR -> true;
            case UNSIGNED_INT, UNSIGNED_LONG, UNSIGNED_CHAR, DOUBLE -> false;
        };
    }

    public boolean isUnsigned() {
        return switch (this.primitive) {
            case UNSIGNED_INT, UNSIGNED_LONG, UNSIGNED_CHAR -> true;
            case INT, LONG, CHAR, SIGNED_CHAR, DOUBLE -> false;
        };
    }
}
