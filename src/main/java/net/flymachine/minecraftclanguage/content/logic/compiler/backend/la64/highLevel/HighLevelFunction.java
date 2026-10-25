package net.flymachine.minecraftclanguage.content.logic.compiler.backend.la64.highLevel;

import net.flymachine.minecraftclanguage.content.logic.architecture.la64.register.GeneralPurposeRegister;
import net.flymachine.minecraftclanguage.content.logic.architecture.la64.register.LA64Register;
import net.flymachine.minecraftclanguage.content.logic.compiler.backend.la64.highLevel.instruction.HighLevelInstruction;
import net.flymachine.minecraftclanguage.content.logic.compiler.backend.la64.highLevel.instruction.Label;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;

/**
 * 栈帧排布如下（某一次调用前）：
 * |      参数..      |
 * |      参数9       |   <- fp
 * |-----------------|
 * |  返回大结构体地址  |   <- fp-8 ?
 * |     保存的ra     |   <- fp-8
 * |     保存的fp     |   <- fp-16
 * | 被调用者保存寄存器  |   <- fp-24
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
     * 函数返回大结构体，其返回结构体的地址保存在 fp-8 处
     */
    public final boolean returnBigStruct;

    /**
     * 保存的寄存器，默认为 $ra 和 $fp
     */
    public final List<LA64Register> savedRegisters = new ArrayList<>();

    /**
     * 栈帧中用于存放临时变量、局部变量、形参所使用的栈帧大小
     */
    public long variableSize = 0;

    /**
     * 该函数的所有函数调用中，参数所占用的栈空间的最大大小值
     */
    public long maxCallStackArgSize = 0;

    public HighLevelFunction(String name, boolean global, boolean returnBigStruct, List<HighLevelInstruction> insts) {
        this.name = name;
        this.global = global;
        this.returnBigStruct = returnBigStruct;
        this.insts = insts;
        this.savedRegisters.add(GeneralPurposeRegister.RA);
        this.savedRegisters.add(GeneralPurposeRegister.FP);
    }

    public long getStackFrameSize() {
        long size = savedRegisters.size() * 8L + (returnBigStruct ? 8L : 0L) + variableSize + maxCallStackArgSize;
        return (size + STACK_ALIGNMENT - 1) & -STACK_ALIGNMENT;
    }

    @Override
    public void dumpPretty(PrintStream stream, int indentLevel, boolean indentFirstLine) {
        if (indentFirstLine) { indent(stream, indentLevel); }
        if (global) { stream.print("global "); }
        stream.print("function #");
        stream.print(name);
        stream.println(" {");
        indent(stream, indentLevel + 1);
        stream.print("#frame=");
        stream.print(getStackFrameSize());
        stream.print(", #localVar=");
        stream.print(variableSize);
        stream.print(", #maxCallStack=");
        stream.println(maxCallStackArgSize);
        for (var instruction : insts) {
            if (instruction instanceof Label l) {
                instruction.dumpPretty(stream, indentLevel, true);
            } else {
                instruction.dumpPretty(stream, indentLevel + 1, true);
            }
            stream.println();
        }
        indent(stream, indentLevel);
        stream.print("}");
    }
}
