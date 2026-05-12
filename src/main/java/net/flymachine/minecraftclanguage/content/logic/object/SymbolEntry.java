package net.flymachine.minecraftclanguage.content.logic.object;

import net.flymachine.minecraftclanguage.content.logic.memory.Segment;

/**
 * 符号表表项
 *
 * @param symbolNameIndex 符号名在符号名列表中的索引
 * @param segment         符号所在的段
 * @param offset          符号相对于所在段起始位置的字节偏移量
 * @param isGlobal        是否为全局符号（可被其他目标文件访问）
 */
public record SymbolEntry(int symbolNameIndex, Segment segment, int offset, boolean isGlobal) { }
