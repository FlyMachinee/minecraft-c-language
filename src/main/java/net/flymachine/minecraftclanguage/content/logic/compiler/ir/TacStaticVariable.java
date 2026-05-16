package net.flymachine.minecraftclanguage.content.logic.compiler.ir;

public class TacStaticVariable implements TacTopLevel {
    public String name;
    public boolean global;
    public int initValue;

    public TacStaticVariable(String name, boolean global, int initValue) {
        this.name = name;
        this.global = global;
        this.initValue = initValue;
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
                     .append(", initValue=")
                     .append(initValue)
                     .append(")");
    }
}
