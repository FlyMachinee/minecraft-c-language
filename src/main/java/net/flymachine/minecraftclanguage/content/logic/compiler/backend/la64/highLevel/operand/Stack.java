package net.flymachine.minecraftclanguage.content.logic.compiler.backend.la64.highLevel.operand;

/**
 * 代表着栈上内存位置
 *
 * @param offset 相对与栈帧底部（fp）的偏移量，栈向地址小的方向生长
 */
public record Stack(int offset) implements HighLevelOperand { }
