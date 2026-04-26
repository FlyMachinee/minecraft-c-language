package net.flymachine.minecraftclanguage.content.logic.assembler.la64.assembly;

import net.flymachine.minecraftclanguage.content.logic.assembler.la64.assembly.operand.LA64AsmOperand;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.StringJoiner;

public record LA64AsmInstruction(String mnemonic, List<LA64AsmOperand> operands) implements LA64AsmStatement {

    @Override
    public @NotNull String toString() {
        if (operands == null || operands.isEmpty()) {
            return mnemonic;
        }
        StringJoiner joiner = new StringJoiner(", ", mnemonic + " ", "");
        for (LA64AsmOperand operand : operands) {
            joiner.add(operand.toString());
        }
        return joiner.toString();
    }

}
