package net.flymachine.minecraftclanguage.content.logic.compiler.ir;

public class TacLabel implements TacInstruction {
    public String name;

    public TacLabel(String name) {
        this.name = name;
    }

    @Override
    public void genFormattedString(StringBuilder stringBuilder, int indentLevel, boolean indentFirstLine) {
        if (indentFirstLine) {
            stringBuilder.append("  ".repeat(indentLevel));
        }
        stringBuilder.append("Label(").append(name).append(")");
    }

    @Override
    public <T> T accept(TacVisitor<T> visitor) {
        return visitor.visit(this);
    }
}
