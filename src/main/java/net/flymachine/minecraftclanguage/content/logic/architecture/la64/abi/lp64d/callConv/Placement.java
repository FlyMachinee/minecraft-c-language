package net.flymachine.minecraftclanguage.content.logic.architecture.la64.abi.lp64d.callConv;

import java.util.List;

public sealed interface Placement {
    /**
     * 通过一组寄存器/栈槽传递，按顺序对应参数各槽
     */
    record InSlots(List<Slot> slots) implements Placement { }

    /**
     * 按引用传递：调用者分配空间，地址通过该槽传递
     */
    record ByReference(Slot pointerSlot) implements Placement { }

    /**
     * 忽略（空结构体、void）
     */
    record Ignored() implements Placement { }
}
