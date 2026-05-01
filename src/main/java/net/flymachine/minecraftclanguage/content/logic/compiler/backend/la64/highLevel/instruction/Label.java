package net.flymachine.minecraftclanguage.content.logic.compiler.backend.la64.highLevel.instruction;

public class Label implements HighLevelInstruction {
    public String identifier;

    public Label(String identifier) {
        this.identifier = identifier;
    }

    @Override
    public <T> T accept(HighLevelVisitor<T> visitor) {
        return visitor.visitLabel(this);
    }
}
