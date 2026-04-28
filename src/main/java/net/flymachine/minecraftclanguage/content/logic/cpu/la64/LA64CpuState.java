package net.flymachine.minecraftclanguage.content.logic.cpu.la64;

public class LA64CpuState {

    public static final int GRLEN = 64;
    public static final long INITIAL_PC = (long) 0x1C000000;

    public LA64CpuState() { }

    private final long[] gr = new long[31];
    private long pc = INITIAL_PC;

    public void reset() {
        pc = INITIAL_PC;
    }

    public long getGr(int regNum) {
        // TODO: 边界检测
        if (regNum == 0) {
            return 0;
        } else {
            return gr[regNum - 1];
        }
    }

    public int getGrWord(int regNum) {
        if (regNum == 0) {
            return 0;
        } else {
            return (int) gr[regNum - 1];
        }
    }

    public void setGr(int regNum, long value) {
        if (regNum != 0) {
            gr[regNum - 1] = value;
        }
    }

    public void setGrUnsignedWord(int regNum, int value) {
        if (regNum != 0) {
            gr[regNum - 1] = value & 0xFFFFFFFFL;
        }
    }

    public long getPc() {
        return pc;
    }

    public long setPc(long nextPc) {
        return pc = nextPc;
    }

    public void pcNext() {
        pc += 4;
    }

    public void pcAdd(long offset) {
        pc += offset;
    }
}
