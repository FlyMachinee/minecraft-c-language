package net.flymachine.minecraftclanguage.content.logic.cpu.la64;

import net.flymachine.minecraftclanguage.content.logic.architecture.la64.register.LA64RegisterResolver;

import java.io.PrintStream;

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

    public void dump(PrintStream out) {
        LA64RegisterResolver resolver = LA64RegisterResolver.getInstance();
        for (int i = 0; i < fr.length; i++) {
            out.printf("f%d(%s):\t%f (0x%016X)%n",
                       i, resolver.getFloatingPointRegister(i).orElseThrow(), getFr(i), getFrL(i));
        }
        for (int i = 0; i < cfr.length; i++) {
            out.printf("cfr%d:\t%b%n", i, cfr[i]);
        }
    }
}
