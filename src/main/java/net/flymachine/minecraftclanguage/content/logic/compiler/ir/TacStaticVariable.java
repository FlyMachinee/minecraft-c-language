package net.flymachine.minecraftclanguage.content.logic.compiler.ir;

public class TacStaticVariable implements TacTopLevel {
    public String identifier;
    public boolean global;
    public int initValue;

    public TacStaticVariable(String identifier, boolean global, int initValue) {
        this.identifier = identifier;
        this.global = global;
        this.initValue = initValue;
    }

    @Override
    public void genFormattedString(StringBuilder stringBuilder, int indentLevel, boolean indentFirstLine) {
        if (indentFirstLine) {
            stringBuilder.append("  ".repeat(indentLevel));
        }
        stringBuilder.append("StaticVar(")
                     .append(identifier)
                     .append(", global=")
                     .append(global)
                     .append(", initValue=")
                     .append(initValue)
                     .append(")");
    }
}
