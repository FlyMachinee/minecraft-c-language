package net.flymachine.minecraftclanguage.content.logic.compiler.common.staticInit;

import net.flymachine.minecraftclanguage.content.logic.compiler.common.util.EscapeUnescapeHelper;
import org.jetbrains.annotations.NotNull;

public record StringInit(byte[] bytes, boolean nullTerminated) implements StaticInit {
    @Override
    public @NotNull String toString() {
        return '\"' + EscapeUnescapeHelper.escapeStringLiteral(bytes) + (nullTerminated ? "\\0\"" : '\"');
    }

    @Override
    public long toByteRepresentation() {
        return 0;
    }
}
