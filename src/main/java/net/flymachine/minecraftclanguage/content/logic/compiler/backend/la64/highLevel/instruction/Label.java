package net.flymachine.minecraftclanguage.content.logic.compiler.backend.la64.highLevel.instruction;

public class Label implements HighLevelInstruction {
    public String name;

    public Label(String name) {
        this.name = name;
    }

    @Override
    public <T> T accept(HighLevelVisitor<T> visitor) {
        return visitor.visitLabel(this);
    }
}
