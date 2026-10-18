package net.flymachine.minecraftclanguage.content.logic.memory.profiler;

import net.flymachine.minecraftclanguage.content.logic.device.la64.Teletypewriter;
import net.flymachine.minecraftclanguage.content.logic.memory.MemoryCrossbar;
import net.flymachine.minecraftclanguage.content.logic.memory.MemoryLikeDevice;
import net.flymachine.minecraftclanguage.content.logic.profiler.CostRecorder;

/**
 * 内存总线装饰器，把每次数据访问与设备访问上报给 {@link CostRecorder}
 * <p>
 * 这里刻意<b>不继承</b> {@code MemoryCrossbar}
 */
public final class MemoryCrossbarWrapper implements MemoryLikeDevice {

    private final MemoryCrossbar delegate;
    private final CostRecorder costSink;

    public MemoryCrossbarWrapper(MemoryCrossbar delegate, CostRecorder costSink) {
        this.delegate = delegate;
        this.costSink = costSink;
    }

    private MemoryLikeDevice resolve(long physicalAddress) {
        return delegate.getDeviceOrThrow(physicalAddress);
    }

    private static boolean isDevice(MemoryLikeDevice device) {
        return device instanceof Teletypewriter;
    }

    @Override
    public byte loadByte(long physicalAddress) {
        MemoryLikeDevice device = resolve(physicalAddress);
        if (isDevice(device)) {
            costSink.recordIo(true);
        } else {
            costSink.recordLoad(1);
        }
        return device.loadByte(physicalAddress);
    }

    @Override
    public byte loadByte(int physicalAddress) {
        return loadByte((long) physicalAddress);
    }

    @Override
    public short loadHalfWord(long physicalAddress) {
        MemoryLikeDevice device = resolve(physicalAddress);
        if (isDevice(device)) {
            costSink.recordIo(true);
        } else {
            costSink.recordLoad(2);
        }
        return device.loadHalfWord(physicalAddress);
    }

    @Override
    public short loadHalfWord(int physicalAddress) {
        return loadHalfWord((long) physicalAddress);
    }

    @Override
    public int loadWord(long physicalAddress) {
        MemoryLikeDevice device = resolve(physicalAddress);
        if (isDevice(device)) {
            costSink.recordIo(true);
        } else {
            costSink.recordLoad(4);
        }
        return device.loadWord(physicalAddress);
    }

    @Override
    public int loadWord(int physicalAddress) {
        return loadWord((long) physicalAddress);
    }

    @Override
    public long loadDoubleWord(long physicalAddress) {
        MemoryLikeDevice device = resolve(physicalAddress);
        if (isDevice(device)) {
            costSink.recordIo(true);
        } else {
            costSink.recordLoad(8);
        }
        return device.loadDoubleWord(physicalAddress);
    }

    @Override
    public long loadDoubleWord(int physicalAddress) {
        return loadDoubleWord((long) physicalAddress);
    }

    @Override
    public void storeByte(long physicalAddress, byte value) {
        MemoryLikeDevice device = resolve(physicalAddress);
        if (isDevice(device)) {
            costSink.recordIo(false);
        } else {
            costSink.recordStore(1);
        }
        device.storeByte(physicalAddress, value);
    }

    @Override
    public void storeByte(int physicalAddress, byte value) {
        storeByte((long) physicalAddress, value);
    }

    @Override
    public void storeHalfWord(long physicalAddress, short value) {
        MemoryLikeDevice device = resolve(physicalAddress);
        if (isDevice(device)) {
            costSink.recordIo(false);
        } else {
            costSink.recordStore(2);
        }
        device.storeHalfWord(physicalAddress, value);
    }

    @Override
    public void storeHalfWord(int physicalAddress, short value) {
        storeHalfWord((long) physicalAddress, value);
    }

    @Override
    public void storeWord(long physicalAddress, int value) {
        MemoryLikeDevice device = resolve(physicalAddress);
        if (isDevice(device)) {
            costSink.recordIo(false);
        } else {
            costSink.recordStore(4);
        }
        device.storeWord(physicalAddress, value);
    }

    @Override
    public void storeWord(int physicalAddress, int value) {
        storeWord((long) physicalAddress, value);
    }

    @Override
    public void storeDoubleWord(long physicalAddress, long value) {
        MemoryLikeDevice device = resolve(physicalAddress);
        if (isDevice(device)) {
            costSink.recordIo(false);
        } else {
            costSink.recordStore(8);
        }
        device.storeDoubleWord(physicalAddress, value);
    }

    @Override
    public void storeDoubleWord(int physicalAddress, long value) {
        storeDoubleWord((long) physicalAddress, value);
    }
}
