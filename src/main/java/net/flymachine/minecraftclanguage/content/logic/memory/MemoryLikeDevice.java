package net.flymachine.minecraftclanguage.content.logic.memory;

public interface MemoryLikeDevice {

    byte loadByte(long physicalAddress);

    byte loadByte(int physicalAddress);

    short loadHalfWord(long physicalAddress);

    short loadHalfWord(int physicalAddress);

    int loadWord(long physicalAddress);

    int loadWord(int physicalAddress);

    long loadDoubleWord(long physicalAddress);

    long loadDoubleWord(int physicalAddress);

    void storeByte(long physicalAddress, byte value);

    void storeByte(int physicalAddress, byte value);

    void storeHalfWord(long physicalAddress, short value);

    void storeHalfWord(int physicalAddress, short value);

    void storeWord(long physicalAddress, int value);

    void storeWord(int physicalAddress, int value);

    void storeDoubleWord(long physicalAddress, long value);

    void storeDoubleWord(int physicalAddress, long value);

}
