package net.flymachine.minecraftclanguage.content.logic.architecture.la64.register;

import net.flymachine.minecraftclanguage.content.logic.compiler.backend.la64.highLevel.operand.HighLevelOperand;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public enum GeneralPurposeRegister implements LA64Register, HighLevelOperand {
    ZERO(0, "zero", "r0"),
    RA(1, "ra", "r1"),
    TP(2, "tp", "r2"),
    SP(3, "sp", "r3"),
    A0(4, "a0", "v0", "r4"),
    A1(5, "a1", "v1", "r5"),
    A2(6, "a2", "r6"),
    A3(7, "a3", "r7"),
    A4(8, "a4", "r8"),
    A5(9, "a5", "r9"),
    A6(10, "a6", "r10"),
    A7(11, "a7", "r11"),
    T0(12, "t0", "r12"),
    T1(13, "t1", "r13"),
    T2(14, "t2", "r14"),
    T3(15, "t3", "r15"),
    T4(16, "t4", "r16"),
    T5(17, "t5", "r17"),
    T6(18, "t6", "r18"),
    T7(19, "t7", "r19"),
    T8(20, "t8", "r20"),
    U0(21, "u0", "r21"),
    FP(22, "fp", "r22"),
    S0(23, "s0", "r23"),
    S1(24, "s1", "r24"),
    S2(25, "s2", "r25"),
    S3(26, "s3", "r26"),
    S4(27, "s4", "r27"),
    S5(28, "s5", "r28"),
    S6(29, "s6", "r29"),
    S7(30, "s7", "r30"),
    S8(31, "s8", "r31");

    private final int number;
    private final List<String> names;

    GeneralPurposeRegister(int number, String... names) {
        this.number = number;
        this.names = Collections.unmodifiableList(Arrays.asList(names));
    }

    @Override
    public RegType getType() {
        return RegType.GPR;
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
