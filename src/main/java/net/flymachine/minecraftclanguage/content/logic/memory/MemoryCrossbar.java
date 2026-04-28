package net.flymachine.minecraftclanguage.content.logic.memory;

public class MemoryCrossbar implements MemoryLikeDevice {
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

    }

    @Override
    public void storeByte(int physicalAddress, byte value) {

    }

    @Override
    public void storeHalfWord(long physicalAddress, short value) {

    }

    @Override
    public void storeHalfWord(int physicalAddress, short value) {

    }

    @Override
    public void storeWord(long physicalAddress, int value) {

    }

    @Override
    public void storeWord(int physicalAddress, int value) {

    }

    @Override
    public void storeDoubleWord(long physicalAddress, long value) {

    }

    @Override
    public void storeDoubleWord(int physicalAddress, long value) {

    }
}
