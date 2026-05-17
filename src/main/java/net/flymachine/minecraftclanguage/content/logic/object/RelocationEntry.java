package net.flymachine.minecraftclanguage.content.logic.object;

/**
 * 重定位表项
 *
 * @param offset          需要重定位的部分相对于所在节起始的字节偏移量
 * @param symbolNameIndex 需要重定位的符号
 * @param relocationType  重定位类型
 */
public record RelocationEntry(int offset, int symbolNameIndex, RelocationType relocationType) { }
