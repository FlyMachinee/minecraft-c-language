package net.flymachine.minecraftclanguage.content.logic.architecture.la64.register;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public enum FloatingPointRegister implements LA64Register {
    F0(RegType.FPR, 0, "f0", "fa0", "fv0"),
    F1(RegType.FPR, 1, "f1", "fa1", "fv1"),
    F2(RegType.FPR, 2, "f2", "fa2"),
    F3(RegType.FPR, 3, "f3", "fa3"),
    F4(RegType.FPR, 4, "f4", "fa4"),
    F5(RegType.FPR, 5, "f5", "fa5"),
    F6(RegType.FPR, 6, "f6", "fa6"),
    F7(RegType.FPR, 7, "f7", "fa7"),
    F8(RegType.FPR, 8, "f8", "ft0"),
    F9(RegType.FPR, 9, "f9", "ft1"),
    F10(RegType.FPR, 10, "f10", "ft2"),
    F11(RegType.FPR, 11, "f11", "ft3"),
    F12(RegType.FPR, 12, "f12", "ft4"),
    F13(RegType.FPR, 13, "f13", "ft5"),
    F14(RegType.FPR, 14, "f14", "ft6"),
    F15(RegType.FPR, 15, "f15", "ft7"),
    F16(RegType.FPR, 16, "f16", "ft8"),
    F17(RegType.FPR, 17, "f17", "ft9"),
    F18(RegType.FPR, 18, "f18", "ft10"),
    F19(RegType.FPR, 19, "f19", "ft11"),
    F20(RegType.FPR, 20, "f20", "ft12"),
    F21(RegType.FPR, 21, "f21", "ft13"),
    F22(RegType.FPR, 22, "f22", "ft14"),
    F23(RegType.FPR, 23, "f23", "ft15"),
    F24(RegType.FPR, 24, "f24", "fs0"),
    F25(RegType.FPR, 25, "f25", "fs1"),
    F26(RegType.FPR, 26, "f26", "fs2"),
    F27(RegType.FPR, 27, "f27", "fs3"),
    F28(RegType.FPR, 28, "f28", "fs4"),
    F29(RegType.FPR, 29, "f29", "fs5"),
    F30(RegType.FPR, 30, "f30", "fs6"),
    F31(RegType.FPR, 31, "f31", "fs7");

    private final RegType type;
    private final int number;
    private final List<String> names;

    FloatingPointRegister(RegType type, int number, String... names) {
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
