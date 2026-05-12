package net.flymachine.minecraftclanguage.content.logic.executable.la64;

/**
 * LA64 架构下的可执行文件
 *
 * @param fileName    可执行文件名
 * @param text        text 段内容
 * @param textVA      text 段起始虚拟地址
 * @param entryOffset 可执行文件执行入口，相对与 text 起始位置的字节偏移量
 */
public record LA64Executable(String fileName, byte[] text, long textVA, int entryOffset) { }
