package net.flymachine.minecraftclanguage.content.logic.compiler.ir;

import java.util.List;

public class TacFunction implements TacTopLevel {
    public String name;
    public boolean global;
    public List<String> params;
    public List<TacInstruction> insts;

    public TacFunction(String name, boolean global, List<String> params, List<TacInstruction> insts) {
        this.name = name;
        this.global = global;
        this.params = params;
        this.insts = insts;
    }

    @Override
    public void genFormattedString(StringBuilder stringBuilder, int indentLevel, boolean indentFirstLine) {
        if (indentFirstLine) {
            stringBuilder.append("  ".repeat(indentLevel));
        }
        stringBuilder.append("TacFunction(\n");
        stringBuilder.append("  ".repeat(indentLevel + 1)).append("name=\"").append(name).append("\",\n");
        stringBuilder.append("  ".repeat(indentLevel + 1)).append("global=").append(global).append(",\n");
        stringBuilder.append("  ".repeat(indentLevel + 1)).append("params=").append(params).append(",\n");
        stringBuilder.append("  ".repeat(indentLevel + 1)).append("instructions=[\n");
        for (TacInstruction instruction : insts) {
            instruction.genFormattedString(stringBuilder, indentLevel + 2, true);
            stringBuilder.append(",\n");
        }
        stringBuilder.append("  ".repeat(indentLevel + 1)).append("]\n");
        stringBuilder.append("  ".repeat(indentLevel)).append(")");
    }
}
