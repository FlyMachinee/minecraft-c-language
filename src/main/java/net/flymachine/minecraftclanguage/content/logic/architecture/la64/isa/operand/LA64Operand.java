package net.flymachine.minecraftclanguage.content.logic.architecture.la64.isa.operand;

import net.flymachine.minecraftclanguage.content.logic.architecture.la64.util.BitMath;

public record LA64Operand(LA64OperandType type, int value) {
    public static LA64Operand reg(int regNumber) {
        return new LA64Operand(LA64OperandType.REG, BitMath.extractBits(regNumber, 5));
    }

    public static LA64Operand si12(int si12) {
        return new LA64Operand(LA64OperandType.SI12, BitMath.extractSignedBits(si12, 12));
    }

    public static LA64Operand offs16(int offs16) {
        return new LA64Operand(LA64OperandType.OFFS16, BitMath.extractSignedBits(offs16, 16));
    }
}
