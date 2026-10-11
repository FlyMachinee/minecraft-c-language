package net.flymachine.minecraftclanguage.content.logic.architecture.la64.abi.lp64d.callConv;

public sealed interface Slot {
    /**
     * 通用寄存器 a0-a7
     */
    record GAR(int index) implements Slot { }

    /**
     * 浮点寄存器 fa0-fa7
     */
    record FAR(int index) implements Slot { }

    /**
     * 栈上偏移（相对函数入口时的 SP）
     */
    record Stack(long offset) implements Slot { }
}
