package net.flymachine.minecraftclanguage.content.logic.memory;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class MemoryCrossbar implements MemoryLikeDevice {

    public MemoryCrossbar(MemoryLikeDevice defaultDevice) {
        this.defaultDevice = defaultDevice;
    }

    public MemoryCrossbar() {
        this.defaultDevice = null;
    }

    private final MemoryLikeDevice defaultDevice;

    private static class MappingEntry {
        long startAddress;
        long endAddress;
        MemoryLikeDevice device;

        public MappingEntry(long startAddress, long endAddress, MemoryLikeDevice device) {
            this.startAddress = startAddress;
            this.endAddress = endAddress;
            this.device = device;
        }

        public boolean matches(long address) {
            return address >= startAddress && address <= endAddress;
        }
    }

    private final List<MappingEntry> entries = new ArrayList<>();

    public void mountDevice(long startAddress, long endAddress, MemoryLikeDevice device) {
        entries.add(new MappingEntry(startAddress, endAddress, device));
    }

    private MemoryLikeDevice getDevice(long address) {
        for (MappingEntry entry : entries) {
            if (entry.matches(address)) {
                return entry.device;
            }
        }
        return defaultDevice;
    }

    private @NotNull MemoryLikeDevice getDeviceOrThrow(long address) {
        MemoryLikeDevice device = getDevice(address);
        if (device == null) {
            throw new IllegalStateException(String.format("No device mapped for address 0x%X", address));
        }
        return device;
    }

    @Override
    public byte loadByte(long physicalAddress) {
        MemoryLikeDevice device = getDeviceOrThrow(physicalAddress);
        return device.loadByte(physicalAddress);
    }

    @Override
    public byte loadByte(int physicalAddress) {
        MemoryLikeDevice device = getDeviceOrThrow(physicalAddress);
        return device.loadByte(physicalAddress);
    }

    @Override
    public short loadHalfWord(long physicalAddress) {
        MemoryLikeDevice device = getDeviceOrThrow(physicalAddress);
        return device.loadHalfWord(physicalAddress);
    }

    @Override
    public short loadHalfWord(int physicalAddress) {
        MemoryLikeDevice device = getDeviceOrThrow(physicalAddress);
        return device.loadHalfWord(physicalAddress);
    }

    @Override
    public int loadWord(long physicalAddress) {
        MemoryLikeDevice device = getDeviceOrThrow(physicalAddress);
        return device.loadWord(physicalAddress);
    }

    @Override
    public int loadWord(int physicalAddress) {
        MemoryLikeDevice device = getDeviceOrThrow(physicalAddress);
        return device.loadWord(physicalAddress);
    }

    @Override
    public long loadDoubleWord(long physicalAddress) {
        MemoryLikeDevice device = getDeviceOrThrow(physicalAddress);
        return device.loadDoubleWord(physicalAddress);
    }

    @Override
    public long loadDoubleWord(int physicalAddress) {
        MemoryLikeDevice device = getDeviceOrThrow(physicalAddress);
        return device.loadDoubleWord(physicalAddress);
    }

    @Override
    public void storeByte(long physicalAddress, byte value) {
        MemoryLikeDevice device = getDeviceOrThrow(physicalAddress);
        device.storeByte(physicalAddress, value);
    }

    @Override
    public void storeByte(int physicalAddress, byte value) {
        MemoryLikeDevice device = getDeviceOrThrow(physicalAddress);
        device.storeByte(physicalAddress, value);
    }

    @Override
    public void storeHalfWord(long physicalAddress, short value) {
        MemoryLikeDevice device = getDeviceOrThrow(physicalAddress);
        device.storeHalfWord(physicalAddress, value);
    }

    @Override
    public void storeHalfWord(int physicalAddress, short value) {
        MemoryLikeDevice device = getDeviceOrThrow(physicalAddress);
        device.storeHalfWord(physicalAddress, value);
    }

    @Override
    public void storeWord(long physicalAddress, int value) {
        MemoryLikeDevice device = getDeviceOrThrow(physicalAddress);
        device.storeWord(physicalAddress, value);
    }

    @Override
    public void storeWord(int physicalAddress, int value) {
        MemoryLikeDevice device = getDeviceOrThrow(physicalAddress);
        device.storeWord(physicalAddress, value);
    }

    @Override
    public void storeDoubleWord(long physicalAddress, long value) {
        MemoryLikeDevice device = getDeviceOrThrow(physicalAddress);
        device.storeDoubleWord(physicalAddress, value);
    }

    @Override
    public void storeDoubleWord(int physicalAddress, long value) {
        MemoryLikeDevice device = getDeviceOrThrow(physicalAddress);
        device.storeDoubleWord(physicalAddress, value);
    }
}
