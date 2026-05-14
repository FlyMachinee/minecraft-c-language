package net.flymachine.minecraftclanguage.content.logic.device.la64;

import net.flymachine.minecraftclanguage.content.logger.ConsoleLogger;
import net.flymachine.minecraftclanguage.content.logger.Logger;
import net.flymachine.minecraftclanguage.content.logic.memory.MemoryLikeDevice;

public final class Teletypewriter implements MemoryLikeDevice {

    public static final int BASE_ADDRESS = 0x1FE00000;
    private final Logger logger;

    public Teletypewriter() {
        logger = new ConsoleLogger();
    }

    @Override
    public byte loadByte(long physicalAddress) {
        return 0;
    }

    @Override
    public byte loadByte(int physicalAddress) {
        return 0;
    }

    @Override
    public short loadHalfWord(long physicalAddress) {
        return 0;
    }

    @Override
    public short loadHalfWord(int physicalAddress) {
        return 0;
    }

    @Override
    public int loadWord(long physicalAddress) {
        return 0;
    }

    @Override
    public int loadWord(int physicalAddress) {
        return 0;
    }

    @Override
    public long loadDoubleWord(long physicalAddress) {
        return 0;
    }

    @Override
    public long loadDoubleWord(int physicalAddress) {
        return 0;
    }

    @Override
    public void storeByte(long physicalAddress, byte value) {
        // 直接输出对应 ASCII 字符
        logger.log(String.valueOf((char) value));
    }

    @Override
    public void storeByte(int physicalAddress, byte value) {
        storeByte((long) physicalAddress, value);
    }

    @Override
    public void storeHalfWord(long physicalAddress, short value) {
        storeByte(physicalAddress, (byte) (value & 0xFF));
    }

    @Override
    public void storeHalfWord(int physicalAddress, short value) {
        storeHalfWord((long) physicalAddress, value);
    }

    @Override
    public void storeWord(long physicalAddress, int value) {
        storeByte(physicalAddress, (byte) (value & 0xFF));
    }

    @Override
    public void storeWord(int physicalAddress, int value) {
        storeWord((long) physicalAddress, value);
    }

    @Override
    public void storeDoubleWord(long physicalAddress, long value) {
        storeByte(physicalAddress, (byte) (value & 0xFF));
    }

    @Override
    public void storeDoubleWord(int physicalAddress, long value) {
        storeDoubleWord((long) physicalAddress, value);
    }
}
