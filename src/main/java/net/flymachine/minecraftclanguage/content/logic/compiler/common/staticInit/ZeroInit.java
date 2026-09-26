package net.flymachine.minecraftclanguage.content.logic.compiler.common.staticInit;

import org.jetbrains.annotations.NotNull;

public final class ZeroInit implements StaticInit {

    private long bytes;

    public ZeroInit(long bytes) {
        this.bytes = bytes;
    }

    public long bytes() {
        return bytes;
    }

    public void expand(long bytes) {
        this.bytes += bytes;
    }

    @Override
    public @NotNull String toString() {
        return "Zero[" + bytes + "]";
    }

    @Override
    public long toByteRepresentation() {
        return 0;
    }
}
