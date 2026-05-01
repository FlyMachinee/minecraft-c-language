package net.flymachine.minecraftclanguage.content.logic.compiler.ir;

public class TacReturn implements TacInstruction {
    public TacValue value;

    public TacReturn(TacValue value) {
        this.value = value;
    }

    @Override
    public void genFormattedString(StringBuilder stringBuilder, int indentLevel, boolean indentFirstLine) {
        if (indentFirstLine) {
            stringBuilder.append("  ".repeat(indentLevel));
        }
        stringBuilder.append("Return(");
        if (value != null) {
            value.genFormattedString(stringBuilder, 0, false);
        }
        stringBuilder.append(")");
    }

    @Override
    public <T> T accept(TacVisitor<T> visitor) {
        return visitor.visit(this);
    }
}
