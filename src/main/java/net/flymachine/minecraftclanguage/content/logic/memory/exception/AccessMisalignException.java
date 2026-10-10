package net.flymachine.minecraftclanguage.content.logic.memory.exception;

public class AccessMisalignException extends MemoryAccessException {
    int alignTo;

    public AccessMisalignException(long badAddress, int alignTo) {
        super(badAddress);
        this.alignTo = alignTo;
    }
}
