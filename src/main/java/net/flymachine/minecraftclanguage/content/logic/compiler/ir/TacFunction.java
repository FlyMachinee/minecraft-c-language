package net.flymachine.minecraftclanguage.content.logic.compiler.ir;

import java.io.PrintStream;
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
    public void dump(PrintStream stream, int indentLevel, boolean indentFirstLine) {
        if (indentFirstLine) {
            stream.print("  ".repeat(indentLevel));
        }
        stream.println("TacFunction(");
        stream.append("  ".repeat(indentLevel + 1)).append("name=").append(name).append(",\n");
        stream.append("  ".repeat(indentLevel + 1)).append("global=").append(String.valueOf(global)).append(",\n");
        stream.append("  ".repeat(indentLevel + 1)).append("params=").append(String.valueOf(params)).append(",\n");
        stream.append("  ".repeat(indentLevel + 1)).append("instructions=[\n");
        for (TacInstruction instruction : insts) {
            instruction.dump(stream, indentLevel + 2, true);
            stream.println(",");
        }
        stream.append("  ".repeat(indentLevel + 1)).append("]\n");
        stream.append("  ".repeat(indentLevel)).append(")");
    }

    @Override
    public void dumpPretty(PrintStream stream, int indentLevel, boolean indentFirstLine) {
        if (indentFirstLine) {
            stream.print("  ".repeat(indentLevel));
        }
        if (global) {
            stream.print("global ");
        }
        stream.append("function ").append(name).append("(");
        for (int i = 0; i < params.size(); i++) {
            if (i > 0) {
                stream.print(", ");
            }
            stream.print(params.get(i));
        }
        stream.println(") {");
        for (TacInstruction instruction : insts) {
            instruction.dumpPretty(stream, indentLevel + 1, true);
            stream.println(";");
        }
        stream.append("  ".repeat(indentLevel)).append("}");
    }
}
