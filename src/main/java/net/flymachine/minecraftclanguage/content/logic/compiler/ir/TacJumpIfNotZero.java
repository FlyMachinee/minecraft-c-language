package net.flymachine.minecraftclanguage.content.logic.compiler.ir;

public class TacJumpIfNotZero implements TacInstruction {
    public TacValue cond;
    public String target;

    public TacJumpIfNotZero(TacValue cond, String target) {
        this.cond = cond;
        this.target = target;
    }

    @Override
    public void genFormattedString(StringBuilder stringBuilder, int indentLevel, boolean indentFirstLine) {
        if (indentFirstLine) {
            stringBuilder.append("  ".repeat(indentLevel));
        }
        stringBuilder.append("JumpIfNotZero(cond=");
        cond.genFormattedString(stringBuilder);
        stringBuilder.append(", target=").append(target).append(")");
    }

    @Override
    public <T> T accept(TacVisitor<T> visitor) {
        return visitor.visit(this);
    }
}
