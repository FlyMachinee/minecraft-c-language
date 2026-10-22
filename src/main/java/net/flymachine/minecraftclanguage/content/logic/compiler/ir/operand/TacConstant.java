package net.flymachine.minecraftclanguage.content.logic.compiler.ir.operand;

import net.flymachine.minecraftclanguage.content.logic.compiler.common.constant.Constant;

import java.io.PrintStream;

public class TacConstant implements TacValue {
    public Constant value;

    public TacConstant(Constant value) {
        this.value = value;
    }

    @Override
    public void dump(PrintStream stream, int indentLevel, boolean indentFirstLine) {
        if (indentFirstLine) {
            indent(stream, indentLevel);
        }
        stream.append("Constant(").append(String.valueOf(value)).append(")");
    }

    @Override
    public void dumpPretty(PrintStream stream, int indentLevel, boolean indentFirstLine) {
        if (indentFirstLine) {
            indent(stream, indentLevel);
        }
        stream.append(String.valueOf(value));
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) { return true; }
        if (!(obj instanceof TacConstant o)) { return false; }
        return value.equals(o.value);
    }

    @Override
    public int hashCode() {
        return value.hashCode();
    }
}
