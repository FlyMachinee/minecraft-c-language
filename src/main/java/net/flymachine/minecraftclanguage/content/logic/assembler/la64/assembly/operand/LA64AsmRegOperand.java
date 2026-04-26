package net.flymachine.minecraftclanguage.content.logic.assembler.la64.assembly.operand;

import net.flymachine.minecraftclanguage.content.logic.architecture.la64.register.LA64Register;
import org.jetbrains.annotations.NotNull;

public record LA64AsmRegOperand(LA64Register reg) implements LA64AsmOperand {

    @Override
    public @NotNull String toString() {
        return reg.getPrimaryName();
    }
}
