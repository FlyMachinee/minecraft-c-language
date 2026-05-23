package net.flymachine.minecraftclanguage.content.logic.compiler.ir;

import java.io.PrintStream;

public class TacJumpIfZero implements TacInstruction {
    public TacValue cond;
    public String target;

    public TacJumpIfZero(TacValue cond, String target) {
        this.cond = cond;
        this.target = target;
    }

    @Override
    public void dump(PrintStream stream, int indentLevel, boolean indentFirstLine) {
        if (indentFirstLine) {
            stream.print("  ".repeat(indentLevel));
        }
        stream.print("JumpIfZero(cond=");
        cond.dump(stream);
        stream.append(", target=").append(target).append(")");
    }

    @Override
    public <T> T accept(TacVisitor<T> visitor) {
        return visitor.visit(this);
    }
}
