package net.flymachine.minecraftclanguage.content.logic.compiler.backend.la64.highLevel.instruction;

public class Branch implements HighLevelInstruction {
    public final String target;

    public Branch(String target) {
        this.target = target;
    }

    @Override
    public <T> T accept(HighLevelVisitor<T> visitor) {
        return visitor.visit(this);
    }
}
