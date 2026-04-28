package net.flymachine.minecraftclanguage.content.logic.memory;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import net.flymachine.minecraftclanguage.content.logic.memory.exception.MemoryAccessMisalignException;

/**
 * 一个简单的 RAM 实现，支持按字节、半字、字和双字加载数据。大小为 4 KB 的整数倍，并且不能超过 4 MB。
 * RAM 的地址空间是循环的，即访问超过 RAM 大小的地址会被对大小取模。
 * 小端序存储，即低地址存储数据的低位，高地址存储数据的高位。
 * 访存不对齐会抛出 {@link MemoryAccessMisalignException} 异常。
 */
public class SimpleRam implements MemoryLikeDevice, DmaDevice {

    public static final int PAGE_SIZE = 4096; // 4 KB

    private final int size;
    private final int pageCount;
    private final Int2ObjectMap<byte[]> pages = new Int2ObjectOpenHashMap<>();

    public SimpleRam(int size) {
        if (size <= 0) {
            throw new IllegalArgumentException("Size must be positive");
        }
        if (size > 1024 * PAGE_SIZE) {
            throw new IllegalArgumentException("Size must be less than 4 MB");
        }

        // 对 PAGE_SIZE 向上取整
        this.size = ((size + PAGE_SIZE - 1) / PAGE_SIZE) * PAGE_SIZE;
        this.pageCount = this.size / PAGE_SIZE;
    }

    public int getSize() {
        return size;
    }

    @Override
    public byte loadByte(long physicalAddress) {
        return loadByte((int) physicalAddress);
    }

    @Override
    public byte loadByte(int physicalAddress) {
        physicalAddress %= size;
        int pageIndex = getPageIndex(physicalAddress);
        int offset = physicalAddress % PAGE_SIZE;
        return getPage(pageIndex)[offset];
    }

    @Override
    public short loadHalfWord(long physicalAddress) {
        return loadHalfWord((int) physicalAddress);
    }

    @Override
    public short loadHalfWord(int physicalAddress) {
        physicalAddress %= size;
        int pageIndex = getPageIndex(physicalAddress);
        int offset = physicalAddress % PAGE_SIZE;
        if (offset % 2 != 0) {
            throw new MemoryAccessMisalignException(physicalAddress, 2);
        }
        byte[] page = getPage(pageIndex);
        return (short) ((page[offset] & 0xFF) | ((page[offset + 1] & 0xFF) << 8));
    }

    @Override
    public int loadWord(long physicalAddress) {
        return loadWord((int) physicalAddress);
    }

    @Override
    public int loadWord(int physicalAddress) {
        physicalAddress %= size;
        int pageIndex = getPageIndex(physicalAddress);
        int offset = physicalAddress % PAGE_SIZE;
        if (offset % 4 != 0) {
            throw new MemoryAccessMisalignException(physicalAddress, 4);
        }
        byte[] page = getPage(pageIndex);
        return (page[offset] & 0xFF) |
               ((page[offset + 1] & 0xFF) << 8) |
               ((page[offset + 2] & 0xFF) << 16) |
               ((page[offset + 3] & 0xFF) << 24);
    }

    @Override
    public long loadDoubleWord(long physicalAddress) {
        return loadDoubleWord((int) physicalAddress);
    }

    @Override
    public long loadDoubleWord(int physicalAddress) {
        physicalAddress %= size;
        int pageIndex = getPageIndex(physicalAddress);
        int offset = physicalAddress % PAGE_SIZE;
        if (offset % 8 != 0) {
            throw new MemoryAccessMisalignException(physicalAddress, 8);
        }
        byte[] page = getPage(pageIndex);
        return (page[offset] & 0xFFL) |
               ((page[offset + 1] & 0xFFL) << 8) |
               ((page[offset + 2] & 0xFFL) << 16) |
               ((page[offset + 3] & 0xFFL) << 24) |
               ((page[offset + 4] & 0xFFL) << 32) |
               ((page[offset + 5] & 0xFFL) << 40) |
               ((page[offset + 6] & 0xFFL) << 48) |
               ((page[offset + 7] & 0xFFL) << 56);
    }

    @Override
    public void storeByte(long physicalAddress, byte value) {
        storeByte((int) physicalAddress, value);
    }

    @Override
    public void storeByte(int physicalAddress, byte value) {
        physicalAddress %= size;
        int pageIndex = getPageIndex(physicalAddress);
        int offset = physicalAddress % PAGE_SIZE;
        getPage(pageIndex)[offset] = value;
    }

    @Override
    public void storeHalfWord(long physicalAddress, short value) {
        storeHalfWord((int) physicalAddress, value);
    }

    @Override
    public void storeHalfWord(int physicalAddress, short value) {
        physicalAddress %= size;
        int pageIndex = getPageIndex(physicalAddress);
        int offset = physicalAddress % PAGE_SIZE;
        if (offset % 2 != 0) {
            throw new MemoryAccessMisalignException(physicalAddress, 2);
        }
        byte[] page = getPage(pageIndex);
        page[offset] = (byte) (value & 0xFF);
        page[offset + 1] = (byte) ((value >> 8) & 0xFF);
    }

    @Override
    public void storeWord(long physicalAddress, int value) {
        storeWord((int) physicalAddress, value);
    }

    @Override
    public void storeWord(int physicalAddress, int value) {
        physicalAddress %= size;
        int pageIndex = getPageIndex(physicalAddress);
        int offset = physicalAddress % PAGE_SIZE;
        if (offset % 4 != 0) {
            throw new MemoryAccessMisalignException(physicalAddress, 4);
        }
        byte[] page = getPage(pageIndex);
        page[offset] = (byte) (value & 0xFF);
        page[offset + 1] = (byte) ((value >> 8) & 0xFF);
        page[offset + 2] = (byte) ((value >> 16) & 0xFF);
        page[offset + 3] = (byte) ((value >> 24) & 0xFF);
    }

    @Override
    public void storeDoubleWord(long physicalAddress, long value) {
        storeDoubleWord((int) physicalAddress, value);
    }

    @Override
    public void storeDoubleWord(int physicalAddress, long value) {
        physicalAddress %= size;
        int pageIndex = getPageIndex(physicalAddress);
        int offset = physicalAddress % PAGE_SIZE;
        if (offset % 8 != 0) {
            throw new MemoryAccessMisalignException(physicalAddress, 8);
        }
        byte[] page = getPage(pageIndex);
        page[offset] = (byte) (value & 0xFFL);
        page[offset + 1] = (byte) ((value >> 8) & 0xFFL);
        page[offset + 2] = (byte) ((value >> 16) & 0xFFL);
        page[offset + 3] = (byte) ((value >> 24) & 0xFFL);
        page[offset + 4] = (byte) ((value >> 32) & 0xFFL);
        page[offset + 5] = (byte) ((value >> 40) & 0xFFL);
        page[offset + 6] = (byte) ((value >> 48) & 0xFFL);
        page[offset + 7] = (byte) ((value >> 56) & 0xFFL);
    }

    private int getPageIndex(long physicalAddress) {
        return (int) (physicalAddress / PAGE_SIZE);
    }

    private byte[] getPage(int pageIndex) {
        return pages.computeIfAbsent(pageIndex, k -> new byte[PAGE_SIZE]);
    }

    @Override
    public void dmaToMemory(long startAddress, byte[] data, int offset, int length) {
        startAddress %= size;
        int pageIndex = getPageIndex(startAddress);
        int pageOffset = ((int) startAddress) % PAGE_SIZE;
        int p = offset;
        byte[] page = getPage(pageIndex);
        for (int i = 0; i < length; i++) {
            page[pageOffset++] = data[p++];
            if (pageOffset == PAGE_SIZE) {
                pageIndex = (pageIndex + 1) % pageCount;
                pageOffset = 0;
                page = getPage(pageIndex);
            }
        }
    }
}
