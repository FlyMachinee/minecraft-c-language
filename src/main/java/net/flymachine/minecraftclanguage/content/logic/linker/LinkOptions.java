package net.flymachine.minecraftclanguage.content.logic.linker;

/**
 * 链接选项
 *
 * @param textVA      text 段起始位置的虚拟地址
 * @param entrySymbol 可执行文件的入口符号名
 */
public record LinkOptions(long textVA, String entrySymbol) {
    public static final LinkOptions DEFAULT = new LinkOptions(0x80000000L, "_start");
    public static final LinkOptions SINGLE = new LinkOptions(0x80000000L, "main");
}
