package net.flymachine.minecraftclanguage.content.logic.compiler.ir;

import net.flymachine.minecraftclanguage.content.logic.compiler.common.Comparison;

public class TacJumpIfComparison implements TacInstruction {
    public Comparison cond;
    public TacValue lhs;
    public TacValue rhs;
    public String target;

    public TacJumpIfComparison(Comparison cond, TacValue lhs, TacValue rhs, String target) {
        this.cond = cond;
        this.lhs = lhs;
        this.rhs = rhs;
        this.target = target;
    }

    @Override
    public void genFormattedString(StringBuilder stringBuilder, int indentLevel, boolean indentFirstLine) {
        if (indentFirstLine) {
            stringBuilder.append("  ".repeat(indentLevel));
        }
        stringBuilder.append("JumpIf(").append(cond).append(", lhs=");
        lhs.genFormattedString(stringBuilder);
        stringBuilder.append(", rhs=");
        rhs.genFormattedString(stringBuilder);
        stringBuilder.append(", target=").append(target).append(")");
    }

    @Override
    public <T> T accept(TacVisitor<T> visitor) {
        return visitor.visit(this);
    }
}
