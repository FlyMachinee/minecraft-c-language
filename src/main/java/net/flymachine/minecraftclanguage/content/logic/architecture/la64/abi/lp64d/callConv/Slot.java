package net.flymachine.minecraftclanguage.content.logic.architecture.la64.abi.lp64d.callConv;

import net.flymachine.minecraftclanguage.content.logic.compiler.backend.la64.highLevel.AsmType;

public sealed interface Slot {

    /**
     * 获取该 Slot 存放的实际数据类型
     */
    AsmType type();

    /**
     * 通用寄存器 a0-a7
     */
    record GAR(int index, AsmType type) implements Slot { }

    /**
     * 浮点寄存器 fa0-fa7
     */
    record FAR(int index, AsmType type) implements Slot { }

    /**
     * 栈上偏移（相对函数入口时的 SP）
     * <p>
     * 注意，根据 ABI，对于标量类型，始终为 8 字节宽度的，由于 ABI 要求进行拓展；这里保留原始类型，请在后续进行拓展
     * <p>
     * 而对于结构体类型，写什么就是什么，ABI 规定额外位是未定义的
     */
    record Stack(long offset, AsmType type) implements Slot { }
}
