package net.flymachine.minecraftclanguage.content.logic.linker;

/**
 * 链接选项
 *
 * @param textVA         text 段起始位置的虚拟地址
 * @param entrySymbol    可执行文件的入口符号名
 * @param stackTopVA     栈顶的虚拟地址
 * @param stackPageCount 栈占用的页数
 */
public record LinkOptions(long textVA, String entrySymbol, long stackTopVA, int stackPageCount) {
    public static final LinkOptions DEFAULT = new LinkOptions(0x80000000L, "_start", 0x00007ffffffffff0L, 4);
}
