package net.flymachine.minecraftclanguage.content.logic.compiler.backend.la64.highLevel.instruction;

import net.flymachine.minecraftclanguage.content.logic.architecture.la64.register.ConditionFlagRegister;

import java.io.PrintStream;

public class BranchIfCCNotZero implements HighLevelInstruction {
    public ConditionFlagRegister cc;
    public String target;

    public BranchIfCCNotZero(ConditionFlagRegister cc, String target) {
        this.cc = cc;
        this.target = target;
    }

    @Override
    public void dumpPretty(PrintStream stream, int indentLevel, boolean indentFirstLine) {
        if (indentFirstLine) {
            indent(stream, indentLevel);
        }
        stream.print("if $");
        stream.print(cc);
        stream.print(" != 0 goto ");
        stream.print(target);
    }

    @Override
    public <T> T accept(HighLevelVisitor<T> visitor) {
        return visitor.visit(this);
    }
}
