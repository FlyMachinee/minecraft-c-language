package net.flymachine.minecraftclanguage.content.logic.compiler.backend.la64.highLevel.instruction;

/**
 *  {@link Call#identifier}，并且将 ra 寄存器设为下一条指令（返回地址）的值
 */
public class Call implements HighLevelInstruction {
    public String identifier;

    public Call(String identifier) {
        this.identifier = identifier;
    }

    @Override
    public <T> T accept(HighLevelVisitor<T> visitor) {
        return visitor.visitCall(this);
    }
}
