package net.flymachine.minecraftclanguage.content.logic.compiler.ir;

import net.flymachine.minecraftclanguage.content.logic.compiler.ir.topLevel.TacTopLevel;

import java.io.PrintStream;
import java.util.List;

public class TacProgram implements TacDataStructure {
    public List<TacTopLevel> topLevels;

    public TacProgram(List<TacTopLevel> topLevels) {
        this.topLevels = topLevels;
    }

    @Override
    public void dump(PrintStream stream, int indentLevel, boolean indentFirstLine) {
        if (indentFirstLine) {
            indent(stream, indentLevel);
        }
        stream.println("TacProgram([");
        for (TacTopLevel topLevel : topLevels) {
            topLevel.dump(stream, indentLevel + 1, true);
            stream.println(",");
        }
        indent(stream, indentLevel);
        stream.print("])");
    }

    @Override
    public void dumpPretty(PrintStream stream, int indentLevel, boolean indentFirstLine) {
        if (indentFirstLine) {
            indent(stream, indentLevel);
        }
        for (TacTopLevel topLevel : topLevels) {
            topLevel.dumpPretty(stream, indentLevel, true);
            stream.println();
        }
    }
}
