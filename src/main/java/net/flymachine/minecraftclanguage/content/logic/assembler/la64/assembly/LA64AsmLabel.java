package net.flymachine.minecraftclanguage.content.logic.assembler.la64.assembly;

import org.jetbrains.annotations.NotNull;

public record LA64AsmLabel(String name) implements LA64AsmStatement {

    @Override
    public @NotNull String toString() {
        return this.name + ":";
    }
}
