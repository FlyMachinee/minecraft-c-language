package net.flymachine.minecraftclanguage.content.logic.compiler.backend.la64.highLevel;

import java.io.PrintStream;
import java.util.List;

public class HighLevelProgram implements HighLevelDataStructure {
    public List<HighLevelTopLevel> topLevels;

    public HighLevelProgram(List<HighLevelTopLevel> topLevels) {
        this.topLevels = topLevels;
    }

    @Override
    public void dumpPretty(PrintStream stream, int indentLevel, boolean indentFirstLine) {
        for (HighLevelTopLevel topLevel : topLevels) {
            topLevel.dumpPretty(stream, indentLevel, true);
            stream.println();
        }
    }
}
