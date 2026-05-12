package net.flymachine.minecraftclanguage.content.logic.compiler.ir;

import java.util.List;

public class TacFunction implements TacDataStructure {
    public String name;
    public List<String> params;
    public List<TacInstruction> instructions;

    public TacFunction(String name, List<String> params, List<TacInstruction> instructions) {
        this.name = name;
        this.params = params;
        this.instructions = instructions;
    }

    @Override
    public void genFormattedString(StringBuilder stringBuilder, int indentLevel, boolean indentFirstLine) {
        if (indentFirstLine) {
            stringBuilder.append("  ".repeat(indentLevel));
        }
        stringBuilder.append("TacFunction(\n");
        stringBuilder.append("  ".repeat(indentLevel + 1)).append("name=\"").append(name).append("\",\n");
        stringBuilder.append("  ".repeat(indentLevel + 1)).append("params=").append(params).append(",\n");
        stringBuilder.append("  ".repeat(indentLevel + 1)).append("instructions=[\n");
        for (TacInstruction instruction : instructions) {
            instruction.genFormattedString(stringBuilder, indentLevel + 2, true);
            stringBuilder.append(",\n");
        }
        stringBuilder.append("  ".repeat(indentLevel + 1)).append("]\n");
        stringBuilder.append("  ".repeat(indentLevel)).append(")");
    }
}
