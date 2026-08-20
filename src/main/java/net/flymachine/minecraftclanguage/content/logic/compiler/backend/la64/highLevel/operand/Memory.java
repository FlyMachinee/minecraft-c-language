package net.flymachine.minecraftclanguage.content.logic.compiler.backend.la64.highLevel.operand;

import net.flymachine.minecraftclanguage.content.logic.architecture.la64.register.GeneralPurposeRegister;

/**
 * 代表内存位置
 *
 * @param gpr    用于寻址的寄存器
 * @param offset 相对于解析器偏移量，栈向地址小的方向生长
 */
public record Memory(GeneralPurposeRegister gpr, int offset) implements HighLevelOperand { }
