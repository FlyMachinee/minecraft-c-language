package net.flymachine.minecraftclanguage.content.logic.compiler.common.type;

import net.flymachine.minecraftclanguage.content.logic.compiler.backend.la64.highLevel.AsmType;
import org.jetbrains.annotations.NotNull;

public final class BasicType extends Type {
    public enum Primitive {
        INT("int"),
        LONG("long"),
        UNSIGNED_INT("unsigned int"),
        UNSIGNED_LONG("unsigned long"),
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
        return this != DOUBLE;
    }

    @Override
    public boolean isReal() {
        return true;
    }

    @Override
    public long sizeof() {
        return switch (this.primitive) {
            case INT, UNSIGNED_INT -> 4;
            case LONG, UNSIGNED_LONG, DOUBLE -> 8;
        };
    }

    @Override
    public AsmType toAsmType() {
        return switch (this.primitive) {
            case INT, UNSIGNED_INT -> AsmType.WORD;
            case LONG, UNSIGNED_LONG -> AsmType.DWORD;
            case DOUBLE -> AsmType.DOUBLE;
        };
    }

    @Override
    public <R> R accept(TypeVisitor<R> visitor) {
        return visitor.visit(this);
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
            default -> throw new IllegalArgumentException("Unknown type: " + typeName);
        };
    }

    public boolean isSigned() {
        return this.primitive == Primitive.INT || this.primitive == Primitive.LONG;
    }

    public boolean isUnsigned() {
        return this.primitive == Primitive.UNSIGNED_INT || this.primitive == Primitive.UNSIGNED_LONG;
    }
}
