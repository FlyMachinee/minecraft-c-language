package net.flymachine.minecraftclanguage.content.logic.compiler.common;

public enum StorageClassSpecifier {
    TYPEDEF, STATIC, EXTERN, AUTO, REGISTER;

    @Override
    public String toString() {
        return name().toLowerCase();
    }

    public static StorageClassSpecifier fromString(String s) {
        return switch (s) {
            case "static" -> STATIC;
            case "extern" -> EXTERN;
            default -> throw new IllegalArgumentException("Invalid storage class specifier: " + s);
        };
    }
}
