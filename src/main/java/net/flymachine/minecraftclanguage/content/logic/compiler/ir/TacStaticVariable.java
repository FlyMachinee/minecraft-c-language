package net.flymachine.minecraftclanguage.content.logic.compiler.ir;

import net.flymachine.minecraftclanguage.content.logic.compiler.common.staticInit.StaticInit;
import net.flymachine.minecraftclanguage.content.logic.compiler.common.type.Type;

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
    public void genFormattedString(StringBuilder stringBuilder, int indentLevel, boolean indentFirstLine) {
        if (indentFirstLine) {
            stringBuilder.append("  ".repeat(indentLevel));
        }
        stringBuilder.append("StaticVar(")
                     .append(name)
                     .append(", global=")
                     .append(global)
                     .append(", type=")
                     .append(type)
                     .append(", initValue=")
                     .append(init)
                     .append(")");
    }
}
