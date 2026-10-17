package net.flymachine.minecraftclanguage.content.logic.compiler.backend.la64.highLevel;

import net.flymachine.minecraftclanguage.content.logic.compiler.common.staticInit.StaticInit;

import java.io.PrintStream;
import java.util.List;

public class HighLevelStaticVar implements HighLevelTopLevel {
    public String name;
    public boolean global;
    public long alignment;
    public List<StaticInit> init;

    public HighLevelStaticVar(String name, boolean global, long alignment, List<StaticInit> init) {
        this.name = name;
        this.global = global;
        this.alignment = alignment;
        this.init = init;
    }

    @Override
    public void dumpPretty(PrintStream stream, int indentLevel, boolean indentFirstLine) {
        if (indentFirstLine) { indent(stream, indentLevel); }
        if (global) { stream.print("global "); }
        stream.print('#');
        stream.print(name);
        stream.print(" [align=");
        stream.print(alignment);
        stream.print("] = ");
        stream.print(init);
    }
}
