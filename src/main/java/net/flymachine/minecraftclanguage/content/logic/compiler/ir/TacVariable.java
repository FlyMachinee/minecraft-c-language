package net.flymachine.minecraftclanguage.content.logic.compiler.ir;

public class TacVariable implements TacValue {
    public String name;

    public TacVariable(String name) {
        this.name = name;
    }

    @Override
    public void genFormattedString(StringBuilder stringBuilder, int indentLevel, boolean indentFirstLine) {
        stringBuilder.append("Var(").append(name).append(")");
    }
}
