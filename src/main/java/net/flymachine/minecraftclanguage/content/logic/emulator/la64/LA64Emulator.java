package net.flymachine.minecraftclanguage.content.logic.emulator.la64;

import net.flymachine.minecraftclanguage.content.logic.architecture.la64.isa.decoder.LA64Decoder;
import net.flymachine.minecraftclanguage.content.logic.architecture.la64.isa.exception.LA64Exception;
import net.flymachine.minecraftclanguage.content.logic.architecture.la64.isa.instruction.LA64Instruction;
import net.flymachine.minecraftclanguage.content.logic.architecture.la64.register.GeneralPurposeRegister;
import net.flymachine.minecraftclanguage.content.logic.cpu.la64.LA64CpuState;
import net.flymachine.minecraftclanguage.content.logic.cpu.la64.LA64MemoryManagementUnit;
import net.flymachine.minecraftclanguage.content.logic.cpu.la64.exception.LA64RuntimeException;
import net.flymachine.minecraftclanguage.content.logic.executable.la64.LA64Executable;
import net.flymachine.minecraftclanguage.content.logic.memory.SimpleRam;

public final class LA64Emulator {

    private final LA64CpuState cpuState = new LA64CpuState();
    private final SimpleRam ram = new SimpleRam(256 * SimpleRam.PAGE_SIZE); // 1 MB
    private final LA64MemoryManagementUnit mmu = new LA64MemoryManagementUnit();

    // 目前，当从 0 地址取指时，停止执行并返回 a0
    private boolean stopFlag = false;

    public LA64Emulator() {
        mmu.addPageTableEntry(
            LA64CpuState.INITIAL_PC / SimpleRam.PAGE_SIZE,
            new LA64MemoryManagementUnit.LA64PageTableEntry(
                0,
                true));
    }

    public void reset() {
        cpuState.reset();
    }

    public long runExecutable(LA64Executable executable) {
        reset();
        loadExecutable(executable);
        return start();
    }

    public void loadExecutable(LA64Executable executable) {
        byte[] textSeg = executable.text();
        ram.dmaToMemory(LA64CpuState.INITIAL_PC, textSeg, 0, textSeg.length);
    }

    public long start() {
        while (!stopFlag) {
            step();
        }
        return cpuState.getGr(GeneralPurposeRegister.A0.getNumber()); // 返回 a0 寄存器的值
    }

    public void step() {
        long fetchVA = cpuState.getPc();

        if (fetchVA % 4 != 0) {
            throw new LA64RuntimeException(LA64Exception.ADEF);
        }

        long fetchPA;
        try {
            fetchPA = mmu.translateVirtualAddress(fetchVA, LA64MemoryManagementUnit.LA64MemoryAccessType.FETCH);
        } catch (LA64RuntimeException e) {
            switch (e.getCode()) {
                case PIF -> {
                    if (fetchVA == 0) {
                        stopFlag = true;
                        return;
                    } else {
                        throw e;
                    }
                }
                default -> throw e;
            }
        }

        int instruction = ram.loadWord(fetchPA);
        LA64Instruction decoded = LA64Decoder.decode(instruction);
        decoded.execute(handler);
    }

    public void step(int cnt) {
        for (int i = 0; i < cnt; i++) {
            step();
            if (stopFlag) {
                break;
            }
        }
    }

    private record LA64EmulatorHandlerImpl(LA64Emulator emulator) implements LA64EmulatorHandler {

        public LA64CpuState getCpuState() {
            return emulator.cpuState;
        }

        public LA64MemoryManagementUnit getMemoryManagementUnit() {
            return emulator.mmu;
        }

        public SimpleRam getRam() {
            return emulator.ram;
        }
    }

    private final LA64EmulatorHandler handler = new LA64EmulatorHandlerImpl(this);
}
