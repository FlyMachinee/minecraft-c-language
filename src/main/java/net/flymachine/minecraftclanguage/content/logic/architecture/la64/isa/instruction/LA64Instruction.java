package net.flymachine.minecraftclanguage.content.logic.architecture.la64.isa.instruction;

import net.flymachine.minecraftclanguage.content.logic.architecture.la64.isa.operand.LA64Operand;
import net.flymachine.minecraftclanguage.content.logic.emulator.la64.LA64EmulatorHandler;
import org.jetbrains.annotations.NotNull;

import java.util.StringJoiner;

public record LA64Instruction(LA64InstructionInfo inst, LA64Operand[] operands) {

    public LA64Instruction(String mnemonic, LA64Operand[] operands) {
        this(
            LA64InstructionSet.getByMnemonic(mnemonic)
                              .orElseThrow(() -> new IllegalArgumentException(
                                  "Unknown instruction mnemonic: " + mnemonic)), operands);
    }

    public void execute(LA64EmulatorHandler emulator) {
        inst.executor().accept(emulator, operands);
    }

    @Override
    public @NotNull String toString() {
        if (operands == null || operands.length == 0) {
            return inst.mnemonic();
        }
        StringJoiner joiner = new StringJoiner(", ", inst.mnemonic() + " ", "");
        for (LA64Operand operand : operands) {
            joiner.add(operand.toString());
        }
        return joiner.toString();
    }
}

