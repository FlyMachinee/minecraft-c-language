package net.flymachine.minecraftclanguage.content.logic.compiler.frontend.c99.enumerate;

public enum TagKind {
    STRUCT("struct"), UNION("union"), ENUM("enum");

    private final String displayName;

    TagKind(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
