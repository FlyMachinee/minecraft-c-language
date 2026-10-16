package net.flymachine.minecraftclanguage.content.logic.compiler.ir.inst;

import net.flymachine.minecraftclanguage.content.logic.compiler.ir.TacVisitor;
import net.flymachine.minecraftclanguage.content.logic.compiler.ir.operand.TacValue;

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
            indent(stream, indentLevel);
        }
        stream.print("JumpIfZero(cond=");
        cond.dump(stream);
        stream.append(", target=").append(target).append(")");
    }

    @Override
    public void dumpPretty(PrintStream stream, int indentLevel, boolean indentFirstLine) {
        if (indentFirstLine) {
            indent(stream, indentLevel);
        }
        stream.append("if (");
        cond.dumpPretty(stream);
        stream.append(" == 0) goto ").append(target);
    }

    @Override
    public <T> T accept(TacVisitor<T> visitor) {
        return visitor.visit(this);
    }
}
