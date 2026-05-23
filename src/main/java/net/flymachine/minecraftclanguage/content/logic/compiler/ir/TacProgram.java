package net.flymachine.minecraftclanguage.content.logic.compiler.ir;

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
            stream.print("  ".repeat(indentLevel));
        }
        stream.println("TacProgram([");
        for (TacTopLevel topLevel : topLevels) {
            topLevel.dump(stream, indentLevel + 1, true);
            stream.println(",");
        }
        stream.print("  ".repeat(indentLevel));
        stream.print("])");
    }
}
