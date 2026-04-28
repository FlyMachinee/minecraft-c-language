package net.flymachine.minecraftclanguage.content.logic.memory.exception;

public class MemoryAccessException extends RuntimeException {
    long badAddress;

    public MemoryAccessException(long badAddress) {
        this.badAddress = badAddress;
    }
}
