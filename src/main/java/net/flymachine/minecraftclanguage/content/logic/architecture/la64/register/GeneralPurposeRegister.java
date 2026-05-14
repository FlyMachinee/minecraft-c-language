package net.flymachine.minecraftclanguage.content.logic.architecture.la64.register;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public enum GeneralPurposeRegister implements LA64Register {
    ZERO(RegType.GPR, 0, "zero", "r0"),
    RA(RegType.GPR, 1, "ra", "r1"),
    TP(RegType.GPR, 2, "tp", "r2"),
    SP(RegType.GPR, 3, "sp", "r3"),
    A0(RegType.GPR, 4, "a0", "v0", "r4"),
    A1(RegType.GPR, 5, "a1", "v1", "r5"),
    A2(RegType.GPR, 6, "a2", "r6"),
    A3(RegType.GPR, 7, "a3", "r7"),
    A4(RegType.GPR, 8, "a4", "r8"),
    A5(RegType.GPR, 9, "a5", "r9"),
    A6(RegType.GPR, 10, "a6", "r10"),
    A7(RegType.GPR, 11, "a7", "r11"),
    T0(RegType.GPR, 12, "t0", "r12"),
    T1(RegType.GPR, 13, "t1", "r13"),
    T2(RegType.GPR, 14, "t2", "r14"),
    T3(RegType.GPR, 15, "t3", "r15"),
    T4(RegType.GPR, 16, "t4", "r16"),
    T5(RegType.GPR, 17, "t5", "r17"),
    T6(RegType.GPR, 18, "t6", "r18"),
    T7(RegType.GPR, 19, "t7", "r19"),
    T8(RegType.GPR, 20, "t8", "r20"),
    U0(RegType.GPR, 21, "u0", "r21"),
    FP(RegType.GPR, 22, "fp", "r22"),
    S0(RegType.GPR, 23, "s0", "r23"),
    S1(RegType.GPR, 24, "s1", "r24"),
    S2(RegType.GPR, 25, "s2", "r25"),
    S3(RegType.GPR, 26, "s3", "r26"),
    S4(RegType.GPR, 27, "s4", "r27"),
    S5(RegType.GPR, 28, "s5", "r28"),
    S6(RegType.GPR, 29, "s6", "r29"),
    S7(RegType.GPR, 30, "s7", "r30"),
    S8(RegType.GPR, 31, "s8", "r31");

    private final RegType type;
    private final int number;
    private final List<String> names;

    GeneralPurposeRegister(RegType type, int number, String... names) {
        this.type = type;
        this.number = number;
        this.names = Collections.unmodifiableList(Arrays.asList(names));
    }

    @Override
    public RegType getType() {
        return type;
    }

    @Override
    public int getNumber() {
        return number;
    }

    @Override
    public String getPrimaryName() {
        return names.get(0);
    }

    @Override
    public List<String> getNames() {
        return names;
    }

    @Override
    public String toString() {
        return getPrimaryName();
    }
}
