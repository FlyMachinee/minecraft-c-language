package net.flymachine.minecraftclanguage.content.logic.cpu.la64;

import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import net.flymachine.minecraftclanguage.content.logic.architecture.la64.isa.exception.LA64Exception;
import net.flymachine.minecraftclanguage.content.logic.cpu.la64.exception.LA64RuntimeException;

public class LA64MemoryManagementUnit {

    public static final int PAGE_SIZE = 4096;

    public LA64MemoryManagementUnit() { }

    private final Long2ObjectMap<LA64PageTableEntry> pageTable = new Long2ObjectOpenHashMap<>();

    public void clearPageTable() {
        pageTable.clear();
    }

    public void addPageTableEntry(long virtualPageNumber, LA64PageTableEntry entry) {
        pageTable.put(virtualPageNumber, entry);
    }

    public long translateVirtualAddress(long virtualAddress, LA64MemoryAccessType accessType) {
        long virtualPageNumber = virtualAddress / PAGE_SIZE;
        long offset = virtualAddress % PAGE_SIZE;
        LA64PageTableEntry entry = pageTable.get(virtualPageNumber);
        if (entry == null) {
            switch (accessType) {
                case FETCH -> throw new LA64RuntimeException(LA64Exception.PIF);
                case LOAD -> throw new LA64RuntimeException(LA64Exception.PIL);
                case STORE -> throw new LA64RuntimeException(LA64Exception.PIS);
            }
        }
        long physicalPageNumber = entry.physicalPageNumber;
        return physicalPageNumber * PAGE_SIZE + offset;
    }

    public static class LA64PageTableEntry {
        public long physicalPageNumber;
        public boolean valid;

        public LA64PageTableEntry(long physicalPageNumber, boolean valid) {
            this.physicalPageNumber = physicalPageNumber;
            this.valid = valid;
        }
    }

    public enum LA64MemoryAccessType {
        FETCH, LOAD, STORE
    }
}
