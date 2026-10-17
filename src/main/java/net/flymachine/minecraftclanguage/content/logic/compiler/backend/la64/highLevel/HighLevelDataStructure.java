package net.flymachine.minecraftclanguage.content.logic.compiler.backend.la64.highLevel;

import java.io.PrintStream;

public interface HighLevelDataStructure {

    default void indent(PrintStream stream, int level) {
        stream.print("    ".repeat(level));
    }

    void dumpPretty(PrintStream stream, int indentLevel, boolean indentFirstLine);

    default void dumpPretty(PrintStream stream) {
        dumpPretty(stream, 0, false);
    }
}
