package net.flymachine.minecraftclanguage.content.logic.compiler.common.type;

import javax.annotation.Nullable;

public final class EnumInfo {
    private final @Nullable String tag;
    private final int id;

    // populate 后填充
    private boolean complete = false;

    public EnumInfo(@Nullable String tag) {
        this.tag = tag;
        this.id = Type.nextTagId();
    }

    public void populate() {
        if (complete) { throw new IllegalStateException("enum is already complete"); }
        this.complete = true;
    }

    public boolean isComplete() { return complete; }

    public @Nullable String tag() { return tag; }

    public int id() { return id; }
}
