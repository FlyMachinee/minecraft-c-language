package net.flymachine.minecraftclanguage.content.logic.compiler.ir;

import net.flymachine.minecraftclanguage.content.logic.compiler.common.Comparison;

import java.io.PrintStream;

public class TacJumpIfComparison implements TacInstruction {
    public Comparison cond;
    public TacValue lhs;
    public TacValue rhs;
    public String target;
    public boolean inverse;

    public TacJumpIfComparison(Comparison cond, TacValue lhs, TacValue rhs, String target, boolean inverse) {
        this.cond = cond;
        this.lhs = lhs;
        this.rhs = rhs;
        this.target = target;
        this.inverse = inverse;
    }

    @Override
    public void dump(PrintStream stream, int indentLevel, boolean indentFirstLine) {
        if (indentFirstLine) {
            stream.print("  ".repeat(indentLevel));
        }
        stream.append("JumpIf(").append(String.valueOf(cond)).append(", lhs=");
        lhs.dump(stream);
        stream.print(", rhs=");
        rhs.dump(stream);
        stream.append(", target=").append(target).append(", inverse=").append(String.valueOf(inverse)).append(")");
    }

    @Override
    public <T> T accept(TacVisitor<T> visitor) {
        return visitor.visit(this);
    }
}
