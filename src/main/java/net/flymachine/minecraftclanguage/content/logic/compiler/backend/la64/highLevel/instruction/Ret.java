package net.flymachine.minecraftclanguage.content.logic.compiler.backend.la64.highLevel.instruction;

public class Ret implements HighLevelInstruction {
    @Override
    public <T> T accept(HighLevelVisitor<T> visitor) {
        return visitor.visitRet(this);
    }
}
