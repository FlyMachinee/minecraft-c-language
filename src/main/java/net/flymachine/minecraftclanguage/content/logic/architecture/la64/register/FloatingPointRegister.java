package net.flymachine.minecraftclanguage.content.logic.architecture.la64.register;

import net.flymachine.minecraftclanguage.content.logic.compiler.backend.la64.highLevel.operand.HighLevelOperand;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public enum FloatingPointRegister implements LA64Register, HighLevelOperand {
    FA0(0, "fa0", "fv0", "f0"),
    FA1(1, "fa1", "fv1", "f1"),
    FA2(2, "fa2", "f2"),
    FA3(3, "fa3", "f3"),
    FA4(4, "fa4", "f4"),
    FA5(5, "fa5", "f5"),
    FA6(6, "fa6", "f6"),
    FA7(7, "fa7", "f7"),
    FT0(8, "ft0", "f8"),
    FT1(9, "ft1", "f9"),
    FT2(10, "ft2", "f10"),
    FT3(11, "ft3", "f11"),
    FT4(12, "ft4", "f12"),
    FT5(13, "ft5", "f13"),
    FT6(14, "ft6", "f14"),
    FT7(15, "ft7", "f15"),
    FT8(16, "ft8", "f16"),
    FT9(17, "ft9", "f17"),
    FT10(18, "ft10", "f18"),
    FT11(19, "ft11", "f19"),
    FT12(20, "ft12", "f20"),
    FT13(21, "ft13", "f21"),
    FT14(22, "ft14", "f22"),
    FT15(23, "ft15", "f23"),
    FS0(24, "fs0", "f24"),
    FS1(25, "fs1", "f25"),
    FS2(26, "fs2", "f26"),
    FS3(27, "fs3", "f27"),
    FS4(28, "fs4", "f28"),
    FS5(29, "fs5", "f29"),
    FS6(30, "fs6", "f30"),
    FS7(31, "fs7", "f31");

    private final int number;
    private final List<String> names;

    FloatingPointRegister(int number, String... names) {
        this.number = number;
        this.names = Collections.unmodifiableList(Arrays.asList(names));
    }

    @Override
    public RegType getType() {
        return RegType.FPR;
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
