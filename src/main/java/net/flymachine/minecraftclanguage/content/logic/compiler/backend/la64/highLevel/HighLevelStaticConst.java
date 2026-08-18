package net.flymachine.minecraftclanguage.content.logic.compiler.backend.la64.highLevel;

import net.flymachine.minecraftclanguage.content.logic.compiler.common.staticInit.StaticInit;

public class HighLevelStaticConst implements HighLevelTopLevel {
    public String name;
    public long alignment;
    public StaticInit staticInit;

    public HighLevelStaticConst(String name, long alignment, StaticInit staticInit) {
        this.name = name;
        this.alignment = alignment;
        this.staticInit = staticInit;
    }
}
