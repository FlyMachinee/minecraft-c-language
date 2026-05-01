package net.flymachine.minecraftclanguage.content.logic.compiler.ir;

public class TacJump implements TacInstruction {
    public String target;

    public TacJump(String target) {
        this.target = target;
    }

    @Override
    public void genFormattedString(StringBuilder stringBuilder, int indentLevel, boolean indentFirstLine) {
        if (indentFirstLine) {
            stringBuilder.append("  ".repeat(indentLevel));
        }
        stringBuilder.append("Jump(").append(target).append(")");
    }

    @Override
    public <T> T accept(TacVisitor<T> visitor) {
        return visitor.visit(this);
    }
}
