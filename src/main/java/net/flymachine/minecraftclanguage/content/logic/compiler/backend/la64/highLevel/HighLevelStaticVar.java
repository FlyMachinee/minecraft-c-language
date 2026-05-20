package net.flymachine.minecraftclanguage.content.logic.compiler.backend.la64.highLevel;

import net.flymachine.minecraftclanguage.content.logic.compiler.common.staticInit.StaticInit;

public class HighLevelStaticVar implements HighLevelTopLevel {
    public String name;
    public boolean global;
    public long alignment;
    public StaticInit init;

    public HighLevelStaticVar(String name, boolean global, long alignment, StaticInit init) {
        this.name = name;
        this.global = global;
        this.alignment = alignment;
        this.init = init;
    }
}
