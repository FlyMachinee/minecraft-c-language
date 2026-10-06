package net.flymachine.minecraftclanguage.content.logic.compiler.ir;

import net.flymachine.minecraftclanguage.content.logic.compiler.common.staticInit.StaticInit;
import net.flymachine.minecraftclanguage.content.logic.compiler.common.type.Type;

import java.io.PrintStream;
import java.util.List;

public class TacStaticVariable implements TacTopLevel {
    public String name;
    public boolean global;
    public Type type;
    public List<StaticInit> init;

    public TacStaticVariable(String name, boolean global, Type type, List<StaticInit> initValue) {
        this.name = name;
        this.global = global;
        this.type = type;
        this.init = initValue;
    }

    @Override
    public void dump(PrintStream stream, int indentLevel, boolean indentFirstLine) {
        if (indentFirstLine) {
            stream.print("  ".repeat(indentLevel));
        }
        stream.append("StaticVar(")
              .append(name)
              .append(", global=")
              .append(String.valueOf(global))
              .append(", type=")
              .append(type.typename())
              .append(", initValue=")
              .append(String.valueOf(init))
              .append(")");
    }

    @Override
    public void dumpPretty(PrintStream stream, int indentLevel, boolean indentFirstLine) {
        if (indentFirstLine) {
            stream.print("  ".repeat(indentLevel));
        }
        if (global) {
            stream.print("global ");
        }
        stream.append(type.typename())
              .append(" ")
              .append(name)
              .append(" = ")
              .append(String.valueOf(init))
              .append(";");
    }
}
