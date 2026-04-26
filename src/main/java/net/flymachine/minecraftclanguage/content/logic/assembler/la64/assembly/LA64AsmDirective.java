package net.flymachine.minecraftclanguage.content.logic.assembler.la64.assembly;

import net.flymachine.minecraftclanguage.content.logic.assembler.la64.assembly.operand.LA64DirectiveArgument;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.StringJoiner;

public record LA64AsmDirective(String name, List<LA64DirectiveArgument> args) implements LA64AsmStatement {

    @Override
    public @NotNull String toString() {
        if (args == null || args.isEmpty()) {
            return "." + name;
        }
        StringJoiner joiner = new StringJoiner(", ", "." + name + " ", "");
        for (LA64DirectiveArgument arg : args) {
            joiner.add(arg.toString());
        }
        return joiner.toString();
    }
}
