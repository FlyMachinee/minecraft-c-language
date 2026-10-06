package net.flymachine.minecraftclanguage.content.logic.assembler.la64.assembly.operand;

import org.jetbrains.annotations.NotNull;

public record LA64DirectiveStrArg(String str) implements LA64DirectiveArgument {

    @Override
    public @NotNull String toString() {
        return "\"" + str + "\"";
    }
}
