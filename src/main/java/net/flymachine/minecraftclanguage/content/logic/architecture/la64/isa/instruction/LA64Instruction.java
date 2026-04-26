package net.flymachine.minecraftclanguage.content.logic.architecture.la64.isa.instruction;

import net.flymachine.minecraftclanguage.content.logic.architecture.la64.isa.operand.LA64Operand;

public record LA64Instruction(LA64InstructionInfo inst, LA64Operand[] operands) {

    public LA64Instruction(String mnemonic, LA64Operand[] operands) {
        this(
            LA64InstructionSet.getByMnemonic(mnemonic)
                              .orElseThrow(() -> new IllegalArgumentException(
                                  "Unknown instruction mnemonic: " + mnemonic)), operands);
    }
}

