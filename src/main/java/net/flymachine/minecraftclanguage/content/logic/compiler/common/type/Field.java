package net.flymachine.minecraftclanguage.content.logic.compiler.common.type;

public final class Field {
    public final String name;
    public final Type type;
    public long offset;

    public Field(String name, Type type) {
        this.name = name;
        this.type = type;
        this.offset = -1;
    }

    public Field(String name, Type type, long offset) {
        this.name = name;
        this.type = type;
        this.offset = offset;
    }
}
