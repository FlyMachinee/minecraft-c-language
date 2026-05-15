package net.flymachine.minecraftclanguage.content.logic.compiler.common.type;

public class BasicType implements Type {
    public enum Kind {
        VOID,
        INT
    }

    private final Kind kind;

    private BasicType(Kind kind) {
        this.kind = kind;
    }

    public Kind getKind() {
        return kind;
    }

    @Override
    public boolean isCompatible(Type other) {
        if (!(other instanceof BasicType)) {
            return false;
        }
        return kind == ((BasicType) other).kind;
    }

    @Override
    public boolean isAssignableFrom(Type other) {
        if (!(other instanceof BasicType)) {
            return false;
        }
        return kind == ((BasicType) other).kind;
    }

    @Override
    public boolean isComplete() {
        return kind != Kind.VOID;
    }

    @Override
    public <R> R accept(TypeVisitor<R> visitor) {
        return visitor.visit(this);
    }

    @Override
    public String toString() {
        return kind.name().toLowerCase();
    }

    public static BasicType fromString(String typeName) {
        return switch (typeName) {
            case "void" -> VOID;
            case "int" -> INT;
            default -> throw new IllegalArgumentException("Unknown basic type: " + typeName);
        };
    }

    public static BasicType VOID = new BasicType(Kind.VOID);
    public static BasicType INT = new BasicType(Kind.INT);
}
