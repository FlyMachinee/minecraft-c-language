package net.flymachine.minecraftclanguage.content.logic.cpu.la64;

public class LA64FpuState {

    public LA64FpuState() { }

    private final double[] fr = new double[32];
    private final boolean[] cfr = new boolean[8];

    public double getFr(int regNum) {
        return fr[regNum];
    }

    public void setFr(int regNum, double value) {
        this.fr[regNum] = value;
    }

    public long getFrL(int regNum) {
        return Double.doubleToRawLongBits(fr[regNum]);
    }

    public void setFrL(int regNum, long value) {
        this.fr[regNum] = Double.longBitsToDouble(value);
    }

    public boolean getCfr(int cc) {
        return cfr[cc];
    }

    public void setCfr(int cc, boolean value) {
        cfr[cc] = value;
    }
}
