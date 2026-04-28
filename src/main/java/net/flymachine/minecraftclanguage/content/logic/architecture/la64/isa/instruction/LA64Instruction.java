package net.flymachine.minecraftclanguage.content.logic.architecture.la64.isa.instruction;

import net.flymachine.minecraftclanguage.content.logic.architecture.la64.isa.operand.LA64Operand;
import net.flymachine.minecraftclanguage.content.logic.emulator.la64.LA64EmulatorHandler;

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
}

