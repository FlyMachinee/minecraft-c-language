package net.flymachine.minecraftclanguage.content.logic.compiler.ir;

public class TacIntConstant implements TacValue {
    public int value;

    public TacIntConstant(int value) {
        this.value = value;
    }

    @Override
    public void genFormattedString(StringBuilder stringBuilder, int indentLevel, boolean indentFirstLine) {
        stringBuilder.append("Constant(").append(value).append(")");
    }
}
