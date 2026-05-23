package net.flymachine.minecraftclanguage.content.logic.compiler.ir;

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
            stream.print("  ".repeat(indentLevel));
        }
        stream.append("Constant(").append(String.valueOf(value)).append(")");
    }
}
