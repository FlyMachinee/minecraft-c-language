package net.flymachine.minecraftclanguage.content.logic.memory.exception;

public class MemoryAccessMisalignException extends MemoryAccessException {
    int alignTo;

    public MemoryAccessMisalignException(long badAddress, int alignTo) {
        super(badAddress);
        this.alignTo = alignTo;
    }
}
