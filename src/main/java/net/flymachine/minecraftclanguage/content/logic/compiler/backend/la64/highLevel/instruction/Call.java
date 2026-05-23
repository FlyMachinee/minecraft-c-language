package net.flymachine.minecraftclanguage.content.logic.compiler.backend.la64.highLevel.instruction;

/**
 * {@link Call#name}，并且将 ra 寄存器设为下一条指令（返回地址）的值
 */
public class Call implements HighLevelInstruction {
    public final String name;

    public Call(String name) {
        this.name = name;
    }

    @Override
    public <T> T accept(HighLevelVisitor<T> visitor) {
        return visitor.visitCall(this);
    }
}
