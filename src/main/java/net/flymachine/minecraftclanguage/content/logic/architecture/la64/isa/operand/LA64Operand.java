package net.flymachine.minecraftclanguage.content.logic.architecture.la64.isa.operand;

import net.flymachine.minecraftclanguage.content.logic.architecture.la64.register.LA64Register;
import net.flymachine.minecraftclanguage.content.logic.architecture.la64.register.LA64RegisterResolver;
import net.flymachine.minecraftclanguage.content.logic.architecture.la64.util.BitMath;
import org.jetbrains.annotations.NotNull;

public record LA64Operand(LA64OperandType type, int value) {
    public LA64Operand {
        value = switch (type) {
            case GPR, FPR -> BitMath.extractBits(value, 5);
            case SI12 -> BitMath.extractSignedBits(value, 12);
            case SI20 -> BitMath.extractSignedBits(value, 20);
            case UI12 -> BitMath.extractBits(value, 12);
            case OFFS16 -> BitMath.extractSignedBits(value, 16);
        };
    }

    @Override
    public @NotNull String toString() {
        return switch (type) {
            case GPR ->
                LA64RegisterResolver.getInstance().getGeneralPurposeRegister(value).orElseThrow().getPrimaryName();
            case FPR ->
                LA64RegisterResolver.getInstance().getFloatingPointRegister(value).orElseThrow().getPrimaryName();
            case SI12, SI20 -> value == 0 ? "0" : String.valueOf(value);
            case UI12 -> value == 0 ? "0" : "0x" + Integer.toHexString(value);
            case OFFS16 -> value == 0 ? "0" : String.valueOf(value << 2);
        };
    }

    public static LA64Operand gpr(int gprNumber) {
        return new LA64Operand(LA64OperandType.GPR, gprNumber);
    }

    public static LA64Operand fpr(int fprNumber) {
        return new LA64Operand(LA64OperandType.FPR, fprNumber);
    }

    public static LA64Operand reg(LA64Register register) {
        return switch (register.getType()) {
            case GPR -> new LA64Operand(LA64OperandType.GPR, register.getNumber());
            case FPR -> new LA64Operand(LA64OperandType.FPR, register.getNumber());
        };
    }

    public static LA64Operand si12(int si12) {
        return new LA64Operand(LA64OperandType.SI12, si12);
    }

    public static LA64Operand si20(int si20) {
        return new LA64Operand(LA64OperandType.SI20, si20);
    }

    public static LA64Operand ui12(int ui12) {
        return new LA64Operand(LA64OperandType.UI12, ui12);
    }

    public static LA64Operand offs16(int offs16) {
        return new LA64Operand(LA64OperandType.OFFS16, offs16);
    }
}
