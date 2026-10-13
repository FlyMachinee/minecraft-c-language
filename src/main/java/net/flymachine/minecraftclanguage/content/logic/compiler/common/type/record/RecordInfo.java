package net.flymachine.minecraftclanguage.content.logic.compiler.common.type.record;

import net.flymachine.minecraftclanguage.content.logic.compiler.common.type.Type;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public abstract class RecordInfo {
    private final @Nullable String tag;
    private final int id;

    protected RecordInfo(@Nullable String tag) {
        this.tag = tag;
        this.id = Type.nextTagId();
    }

    // populate 后进行填充
    private List<Field> fields;
    private final Map<String, Field> fieldMap = new HashMap<>();
    private boolean complete = false;

    protected long size;
    protected long alignment;

    public final @Nullable String tag() { return tag; }

    public final int id() { return id; }

    public final boolean isComplete() { return complete; }

    public final void populate(List<Field> fields) {
        if (complete) {
            throw new IllegalStateException("struct/union is already complete");
        }
        this.fields = fields;
        for (Field field : fields) {
            fieldMap.put(field.name, field);
        }
        complete = true;
        computeLayout(fields);
    }

    protected abstract void computeLayout(List<Field> fields);

    public final List<Field> fields() {
        checkComplete();
        return fields;
    }

    public final boolean hasField(String fieldName) {
        checkComplete();
        return fieldMap.containsKey(fieldName);
    }

    public final Optional<Field> getField(String fieldName) {
        checkComplete();
        return Optional.ofNullable(fieldMap.get(fieldName));
    }

    public final long sizeof() {
        checkComplete();
        return size;
    }

    public final long alignof() {
        checkComplete();
        return alignment;
    }

    private void checkComplete() {
        if (!complete) {
            throw new IllegalStateException("struct/union is not complete yet");
        }
    }
}
