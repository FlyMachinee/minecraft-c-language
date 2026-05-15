package net.flymachine.minecraftclanguage.content.logic.compiler.ir;

import java.util.List;

public class TacProgram implements TacDataStructure {
    public List<TacTopLevel> topLevels;

    public TacProgram(List<TacTopLevel> topLevels) {
        this.topLevels = topLevels;
    }

    @Override
    public void genFormattedString(StringBuilder stringBuilder, int indentLevel, boolean indentFirstLine) {
        if (indentFirstLine) {
            stringBuilder.append("  ".repeat(indentLevel));
        }
        stringBuilder.append("TacProgram([\n");
        for (TacTopLevel topLevel : topLevels) {
            topLevel.genFormattedString(stringBuilder, indentLevel + 1, true);
            stringBuilder.append(",\n");
        }
        stringBuilder.append("  ".repeat(indentLevel)).append("])");
    }
}
