package net.flymachine.minecraftclanguage.content.logic.compiler.ir;

import net.flymachine.minecraftclanguage.content.logic.compiler.common.staticInit.StaticInit;
import net.flymachine.minecraftclanguage.content.logic.compiler.common.type.Type;

import java.io.PrintStream;

public class TacStaticVariable implements TacTopLevel {
    public String name;
    public boolean global;
    public Type type;
    public StaticInit init;

    public TacStaticVariable(String name, boolean global, Type type, StaticInit initValue) {
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
              .append(String.valueOf(type))
              .append(", initValue=")
              .append(String.valueOf(init))
              .append(")");
    }
}
