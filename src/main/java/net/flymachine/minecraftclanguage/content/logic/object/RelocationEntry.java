package net.flymachine.minecraftclanguage.content.logic.object;

/**
 * 重定位表项
 *
 * @param offset          需要重定位的偏移量
 * @param symbolNameIndex 需要重定位的符号
 * @param relocationType  重定位类型
 * @param addend          重定位加数
 */
public record RelocationEntry(long offset, int symbolNameIndex, RelocationType relocationType, long addend) {

    public RelocationEntry(long offset, int symbolNameIndex, RelocationType relocationType) {
        this(offset, symbolNameIndex, relocationType, 0);
    }
}
