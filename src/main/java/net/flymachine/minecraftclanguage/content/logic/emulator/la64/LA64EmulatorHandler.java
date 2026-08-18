package net.flymachine.minecraftclanguage.content.logic.emulator.la64;

import net.flymachine.minecraftclanguage.content.logic.cpu.la64.LA64CpuState;
import net.flymachine.minecraftclanguage.content.logic.cpu.la64.LA64FpuState;
import net.flymachine.minecraftclanguage.content.logic.cpu.la64.LA64MemoryManagementUnit;
import net.flymachine.minecraftclanguage.content.logic.memory.MemoryLikeDevice;

public interface LA64EmulatorHandler {
    LA64CpuState getCpuState();

    LA64MemoryManagementUnit getMemoryManagementUnit();

    MemoryLikeDevice getMemory();

    LA64FpuState getFpuState();
}
