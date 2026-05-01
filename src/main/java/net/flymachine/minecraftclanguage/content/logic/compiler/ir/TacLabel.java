package net.flymachine.minecraftclanguage.content.logic.compiler.ir;

public class TacLabel implements TacInstruction {
    public String identifier;

    public TacLabel(String identifier) {
        this.identifier = identifier;
    }

    @Override
    public void genFormattedString(StringBuilder stringBuilder, int indentLevel, boolean indentFirstLine) {
        if (indentFirstLine) {
            stringBuilder.append("  ".repeat(indentLevel));
        }
        stringBuilder.append("Label(").append(identifier).append(")");
    }

    @Override
    public <T> T accept(TacVisitor<T> visitor) {
        return visitor.visit(this);
    }
}
