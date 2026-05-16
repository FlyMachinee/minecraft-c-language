package net.flymachine.minecraftclanguage.content.logic.compiler.backend.la64.highLevel;

import net.flymachine.minecraftclanguage.content.logic.compiler.backend.la64.highLevel.instruction.HighLevelInstruction;

import java.util.List;

/**
 * 栈帧排布如下（某一次调用前）：
 * |      参数..      |
 * |      参数9       |   <- fp
 * |-----------------|
 * |     保存的ra     |   <- fp-8
 * |     保存的fp     |   <- fp-16
 * |   局部、临时变量   |
 * |     padding     |
 * |     参数...      |   <- sp+..
 * |      参数10      |   <- sp+8
 * |      参数9       |   <- sp
 * |-----------------|
 * 注：sp/fp只会在函数的序言中设置，在整个函数中再也不会变化
 */

public class HighLevelFunction implements HighLevelTopLevel {
    public String name;
    public boolean global;
    public List<HighLevelInstruction> insts;

    /**
     * 栈指针要求以 16 字节对齐
     */
    public static final int STACK_ALIGNMENT = 16;

    /**
     * 是否使用帧指针 $fp 寻址
     */
    public final boolean useFp = true;

    /**
     * 保存寄存器的数量，目前为 $ra 和 $fp
     */
    public final int savedRegisters = 2;

    /**
     * 栈帧中用于存放临时变量、局部变量、形参所使用的栈帧大小
     */
    public int variableSize = 0;

    /**
     * 该函数的所有函数调用中，参数所占用的栈空间的最大大小值
     * <p>
     * 参数顺序 a0 -> a1 -> ... -> a7 -> [0(sp) -> 8(sp) -> ...]
     */
    public int maxCallStackArgSize = 0;

    public HighLevelFunction(String name, boolean global, List<HighLevelInstruction> insts) {
        this.name = name;
        this.global = global;
        this.insts = insts;
    }

    public int getStackFrameSize() {
        int size = savedRegisters * 8 + variableSize + maxCallStackArgSize;
        return (size + STACK_ALIGNMENT - 1) & -STACK_ALIGNMENT;
    }
}

