package net.flymachine.minecraftclanguage.content.logic.object.la64;

/**
 * 重定位表项
 *
 * @param textOffset      需要重定位的指令相对于 text 段起始的字节偏移量
 * @param symbolNameIndex 需要重定位的符号
 * @param relocationType  重定位类型
 */
public record RelocationEntry(int textOffset, int symbolNameIndex, RelocationType relocationType) { }
