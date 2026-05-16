package net.flymachine.minecraftclanguage.content.logic.assembler.la64.assembly.operand;

import net.flymachine.minecraftclanguage.content.logic.architecture.la64.register.FloatingPointRegister;
import net.flymachine.minecraftclanguage.content.logic.architecture.la64.register.GeneralPurposeRegister;

public interface LA64AsmOperand {
    default boolean isGpr() {
        return this instanceof GeneralPurposeRegister;
    }

    default boolean isFpr() {
        return this instanceof FloatingPointRegister;
    }

    default boolean isImm() {
        return this instanceof LA64AsmImmOperand;
    }

    default boolean isSym() {
        return this instanceof LA64AsmSymOperand;
    }

    default GeneralPurposeRegister asGpr() {
        return (GeneralPurposeRegister) this;
    }

    default GeneralPurposeRegister asFpr() {
        return (GeneralPurposeRegister) this;
    }

    default long asImm() {
        return ((LA64AsmImmOperand) this).value();
    }

    default String asSym() {
        return ((LA64AsmSymOperand) this).symbol();
    }
}
