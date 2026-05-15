package net.flymachine.minecraftclanguage.content.logic.compiler.backend.la64.highLevel;

public class HighLevelStaticVar implements HighLevelTopLevel {
    public String name;
    public boolean global;
    public int initValue;

    public HighLevelStaticVar(String name, boolean global, int initValue) {
        this.name = name;
        this.global = global;
        this.initValue = initValue;
    }
}
