package net.flymachine.minecraftclanguage.content.logic.architecture.la64.register;

import java.util.List;

public enum ConditionFlagRegister implements LA64Register {
    FCC0(0, "fcc0"),
    FCC1(1, "fcc1"),
    FCC2(2, "fcc2"),
    FCC3(3, "fcc3"),
    FCC4(4, "fcc4"),
    FCC5(5, "fcc5"),
    FCC6(6, "fcc6"),
    FCC7(7, "fcc7");

    private final int number;
    private final String name;

    ConditionFlagRegister(int number, String name) {
        this.number = number;
        this.name = name;
    }

    @Override
    public RegType getType() {
        return RegType.CFR;
    }

    @Override
    public int getNumber() {
        return number;
    }

    @Override
    public String getPrimaryName() {
        return name;
    }

    @Override
    public List<String> getNames() {
        return List.of(name);
    }

    @Override
    public String toString() {
        return getPrimaryName();
    }
}
