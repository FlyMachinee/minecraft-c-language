package net.flymachine.minecraftclanguage.content.logic.architecture.la64.isa.operand;

import net.flymachine.minecraftclanguage.content.logic.architecture.la64.register.LA64Register;
import net.flymachine.minecraftclanguage.content.logic.architecture.la64.util.BitMath;

public record LA64Operand(LA64OperandType type, int value) {
    public LA64Operand {
        value = switch (type) {
            case REG -> BitMath.extractBits(value, 5);
            case SI12 -> BitMath.extractSignedBits(value, 12);
            case OFFS16 -> BitMath.extractSignedBits(value, 16);
        };
    }

    public static LA64Operand reg(int regNumber) {
        return new LA64Operand(LA64OperandType.REG, regNumber);
    }

    public static LA64Operand reg(LA64Register register) {
        return reg(register.getNumber());
    }

    public static LA64Operand si12(int si12) {
        return new LA64Operand(LA64OperandType.SI12, si12);
    }

    public static LA64Operand offs16(int offs16) {
        return new LA64Operand(LA64OperandType.OFFS16, offs16);
    }
}
