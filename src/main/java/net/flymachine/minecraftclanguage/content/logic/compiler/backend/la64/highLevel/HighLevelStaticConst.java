package net.flymachine.minecraftclanguage.content.logic.compiler.backend.la64.highLevel;

import net.flymachine.minecraftclanguage.content.logic.compiler.common.staticInit.StaticInit;

import java.io.PrintStream;

public class HighLevelStaticConst implements HighLevelTopLevel {
    public String name;
    public long alignment;
    public StaticInit staticInit;

    public HighLevelStaticConst(String name, long alignment, StaticInit staticInit) {
        this.name = name;
        this.alignment = alignment;
        this.staticInit = staticInit;
    }

    @Override
    public void dumpPretty(PrintStream stream, int indentLevel, boolean indentFirstLine) {
        if (indentFirstLine) { indent(stream, indentLevel); }
        stream.print("constexpr #");
        stream.print(name);
        stream.print(" [align=");
        stream.print(alignment);
        stream.print("] = ");
        stream.print(staticInit);
    }
}
