package net.flymachine.minecraftclanguage.content.logic.emulator.la64;

import net.flymachine.minecraftclanguage.content.logic.architecture.la64.isa.decoder.LA64Decoder;
import net.flymachine.minecraftclanguage.content.logic.architecture.la64.isa.exception.LA64Exception;
import net.flymachine.minecraftclanguage.content.logic.architecture.la64.isa.instruction.LA64Instruction;
import net.flymachine.minecraftclanguage.content.logic.architecture.la64.register.GeneralPurposeRegister;
import net.flymachine.minecraftclanguage.content.logic.cpu.la64.LA64CpuState;
import net.flymachine.minecraftclanguage.content.logic.cpu.la64.LA64MemoryManagementUnit;
import net.flymachine.minecraftclanguage.content.logic.cpu.la64.exception.LA64RuntimeException;
import net.flymachine.minecraftclanguage.content.logic.device.la64.Teletypewriter;
import net.flymachine.minecraftclanguage.content.logic.executable.la64.LA64Executable;
import net.flymachine.minecraftclanguage.content.logic.memory.MemoryCrossbar;
import net.flymachine.minecraftclanguage.content.logic.memory.MemoryLikeDevice;
import net.flymachine.minecraftclanguage.content.logic.memory.SimpleRam;

public final class LA64Emulator {

    private final LA64CpuState cpuState = new LA64CpuState();
    private final SimpleRam ram = new SimpleRam(256 * SimpleRam.PAGE_SIZE); // 1 MB
    private final MemoryCrossbar memoryCrossbar = new MemoryCrossbar(ram);
    private final LA64MemoryManagementUnit mmu = new LA64MemoryManagementUnit();

    private int ppn = 0;

    // 目前，当从 0 地址取指时，停止执行并返回 a0
    private boolean stopFlag = false;

    public LA64Emulator() {
        reset();
        Teletypewriter tty = new Teletypewriter();
        memoryCrossbar.amountDevice(
            Teletypewriter.BASE_ADDRESS, Teletypewriter.BASE_ADDRESS + SimpleRam.PAGE_SIZE - 1, tty);
    }

    public void reset() {
        cpuState.reset();
        mmu.clearPageTable();
        ppn = 0;
        long ttyPageNumber = Teletypewriter.BASE_ADDRESS / SimpleRam.PAGE_SIZE;
        mmu.addPageTableEntry(ttyPageNumber, new LA64MemoryManagementUnit.LA64PageTableEntry(ttyPageNumber, true));
        stopFlag = false;
    }

    public long runExecutable(LA64Executable executable) {
        reset();
        loadExecutable(executable);
        return start();
    }

    public void loadExecutable(LA64Executable executable) {
        byte[] textSeg = executable.text();
        long textVA = executable.textVA();
        long textVPN = textVA / SimpleRam.PAGE_SIZE;
        long testPageCount = (textSeg.length + SimpleRam.PAGE_SIZE - 1) / SimpleRam.PAGE_SIZE;
        for (int i = 0; i < testPageCount; ++i) {
            mmu.addPageTableEntry(
                textVPN + i,
                new LA64MemoryManagementUnit.LA64PageTableEntry(ppn++, true));
            long textPA = mmu.translateVirtualAddress(
                textVA + (long) i * SimpleRam.PAGE_SIZE,
                LA64MemoryManagementUnit.LA64MemoryAccessType.FETCH);
            int offset = i * SimpleRam.PAGE_SIZE;
            int length = Math.min(SimpleRam.PAGE_SIZE, textSeg.length - offset);
            ram.dmaToMemory(textPA, textSeg, offset, length);
        }

        long stackVPN = executable.stackTopVA() / SimpleRam.PAGE_SIZE;
        for (int i = 0; i < executable.stackPageCount(); ++i) {
            mmu.addPageTableEntry(
                stackVPN - i,
                new LA64MemoryManagementUnit.LA64PageTableEntry(ppn++, true));
        }
        cpuState.setPc(executable.textVA() + executable.entryOffset());
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

        public MemoryLikeDevice getMemory() {
            return emulator.memoryCrossbar;
        }
    }

    private final LA64EmulatorHandler handler = new LA64EmulatorHandlerImpl(this);
}
