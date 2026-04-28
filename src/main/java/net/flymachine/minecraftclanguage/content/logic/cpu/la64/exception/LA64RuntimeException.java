package net.flymachine.minecraftclanguage.content.logic.cpu.la64.exception;

import net.flymachine.minecraftclanguage.content.logic.architecture.la64.isa.exception.LA64Exception;

public class LA64RuntimeException extends RuntimeException {
    private final LA64Exception code;

    public LA64RuntimeException(LA64Exception code) {
        this.code = code;
    }

    public LA64Exception getCode() {
        return code;
    }
}
