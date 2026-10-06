package net.flymachine.minecraftclanguage.content.logic.compiler.ir;

import net.flymachine.minecraftclanguage.content.logic.compiler.common.staticInit.StaticInit;
import net.flymachine.minecraftclanguage.content.logic.compiler.common.type.Type;

import java.io.PrintStream;

public class TacStaticConstant implements TacTopLevel {
    public String name;
    public Type type;
    public StaticInit init;

    public TacStaticConstant(String name, Type type, StaticInit initValue) {
        this.name = name;
        this.type = type;
        this.init = initValue;
    }

    @Override
    public void dump(PrintStream stream, int indentLevel, boolean indentFirstLine) {
        if (indentFirstLine) {
            stream.print("  ".repeat(indentLevel));
        }
        stream.append("StaticConst(")
              .append(name)
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
        // constexpr type name = init;
        stream.append("constexpr ")
              .append(type.typename())
              .append(" ")
              .append(name)
              .append(" = ")
              .append(String.valueOf(init))
              .append(";");
    }
}
