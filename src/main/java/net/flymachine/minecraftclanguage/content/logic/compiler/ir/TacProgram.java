package net.flymachine.minecraftclanguage.content.logic.compiler.ir;

import java.util.List;

public class TacProgram implements TacDataStructure {
    public List<TacFunction> functionDefinitions;

    public TacProgram(List<TacFunction> functionDefinitions) {
        this.functionDefinitions = functionDefinitions;
    }

    @Override
    public void genFormattedString(StringBuilder stringBuilder, int indentLevel, boolean indentFirstLine) {
        if (indentFirstLine) {
            stringBuilder.append("  ".repeat(indentLevel));
        }
        stringBuilder.append("TacProgram([\n");
        for (TacFunction function : functionDefinitions) {
            function.genFormattedString(stringBuilder, indentLevel + 1, true);
            stringBuilder.append(",\n");
        }
        stringBuilder.append("  ".repeat(indentLevel)).append("])");
    }
}
