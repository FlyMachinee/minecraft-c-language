package net.flymachine.minecraftclanguage.content.logic.memory;

public interface DmaDevice {

    /**
     * 将 {@code data[offset:offset+length]} 的区间值直接写入内存中
     *
     * @param startAddress 写入的起始内存地址
     * @param data         写入的数据
     * @param offset       写入数据的偏移
     * @param length       写入数据的长度
     */
    void dmaToMemory(long startAddress, byte[] data, int offset, int length);
}
