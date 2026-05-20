package net.flymachine.minecraftclanguage.content.logic.assembler.la64.assembly.operand;

import net.flymachine.minecraftclanguage.content.logic.compiler.backend.la64.highLevel.operand.Immediate;
import org.jetbrains.annotations.NotNull;

public record LA64AsmImmOperand(long value) implements LA64AsmOperand {

    public LA64AsmImmOperand(Immediate imm) {
        this(imm.value());
    }

    public static final LA64AsmImmOperand ZERO = new LA64AsmImmOperand(0);

    @Override
    public @NotNull String toString() {
        return String.valueOf(value);
    }
}
