package net.flymachine.minecraftclanguage.content.logic.compiler.ir;

public class TacVariable implements TacValue {
    public String identifier;

    public TacVariable(String identifier) {
        this.identifier = identifier;
    }

    @Override
    public void genFormattedString(StringBuilder stringBuilder, int indentLevel, boolean indentFirstLine) {
        stringBuilder.append("Var(").append(identifier).append(")");
    }
}
