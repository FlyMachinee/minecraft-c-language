package net.flymachine.minecraftclanguage.content.logic.cpu.la64.profiler;

import net.flymachine.minecraftclanguage.content.logic.cpu.la64.LA64MemoryManagementUnit;
import net.flymachine.minecraftclanguage.content.logic.profiler.CostRecorder;

/**
 * MMU 装饰器，把每次地址翻译上报给 {@link CostRecorder}
 */
public final class LA64MmuWrapper extends LA64MemoryManagementUnit {

    private final LA64MemoryManagementUnit delegate;
    private final CostRecorder costSink;

    public LA64MmuWrapper(LA64MemoryManagementUnit delegate, CostRecorder costSink) {
        this.delegate = delegate;
        this.costSink = costSink;
    }

    @Override
    public long translateVirtualAddress(long virtualAddress, LA64MemoryAccessType accessType) {
        long physicalAddress = delegate.translateVirtualAddress(virtualAddress, accessType);
        switch (accessType) {
            case FETCH -> costSink.recordFetchTranslation();
            case LOAD -> costSink.recordLoadTranslation();
            case STORE -> costSink.recordStoreTranslation();
        }
        return physicalAddress;
    }

    @Override
    public void clearPageTable() {
        delegate.clearPageTable();
    }

    @Override
    public void addPageTableEntry(long virtualPageNumber, LA64PageTableEntry entry) {
        delegate.addPageTableEntry(virtualPageNumber, entry);
    }
}
