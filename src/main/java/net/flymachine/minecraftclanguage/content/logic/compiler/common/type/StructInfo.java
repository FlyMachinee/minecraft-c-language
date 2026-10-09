package net.flymachine.minecraftclanguage.content.logic.compiler.common.type;

import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class StructInfo {
    private final @Nullable String tag;
    private final int id;

    public StructInfo(@Nullable String tag) {
        this.tag = tag;
        this.id = Type.nextTagId();
    }

    // populate 后进行填充
    private List<Field> fields;
    private final Map<String, Field> fieldMap = new HashMap<>();
    private long size;
    private long alignment;
    private boolean complete = false;

    public @Nullable String tag() { return tag; }

    public int id() { return id; }

    public boolean isComplete() { return complete; }

    public void populate(List<Field> fields, long size, long alignment) {
        if (complete) {
            throw new IllegalStateException("struct is already complete");
        }
        this.fields = fields;
        this.size = size;
        this.alignment = alignment;
        for (Field field : fields) {
            fieldMap.put(field.name, field);
        }
        complete = true;
    }

    public List<Field> fields() {
        if (!complete) {
            throw new IllegalStateException("struct is not complete yet");
        }
        return fields;
    }

    public Field getField(String fieldName) {
        if (!complete) {
            throw new IllegalStateException("struct is not complete yet");
        }
        return fieldMap.get(fieldName);
    }

    public long sizeof() {
        if (!complete) {
            throw new IllegalStateException("struct is not complete yet");
        }
        return size;
    }

    public long alignof() {
        if (!complete) {
            throw new IllegalStateException("struct is not complete yet");
        }
        return alignment;
    }
}
