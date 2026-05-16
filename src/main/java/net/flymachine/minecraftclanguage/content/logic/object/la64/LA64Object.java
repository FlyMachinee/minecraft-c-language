package net.flymachine.minecraftclanguage.content.logic.object.la64;

import net.flymachine.minecraftclanguage.content.logic.object.SymbolEntry;

import java.util.List;

/**
 * LA64 架构下的目标文件
 *
 * @param fileName    文件名
 * @param text        text 段内容
 * @param data        data 段内容
 * @param bssSize     bss 段大小
 * @param symbols     符号表
 * @param relocations 重定位表
 * @param symbolNames 符号名列表，符号表项中的 symbolNameIndex 字段是该列表的索引
 */
public record LA64Object(
    String fileName,
    byte[] text,
    byte[] data,
    int bssSize,
    List<SymbolEntry> symbols,
    List<RelocationEntry> relocations,
    List<String> symbolNames) { }
