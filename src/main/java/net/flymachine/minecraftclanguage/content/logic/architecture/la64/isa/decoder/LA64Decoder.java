package net.flymachine.minecraftclanguage.content.logic.architecture.la64.isa.decoder;

import net.flymachine.minecraftclanguage.content.logic.architecture.la64.isa.exception.LA64Exception;
import net.flymachine.minecraftclanguage.content.logic.architecture.la64.isa.instruction.LA64Instruction;
import net.flymachine.minecraftclanguage.content.logic.architecture.la64.isa.instruction.LA64InstructionInfo;
import net.flymachine.minecraftclanguage.content.logic.architecture.la64.isa.instruction.LA64InstructionSet;
import net.flymachine.minecraftclanguage.content.logic.architecture.la64.isa.operand.LA64Operand;
import net.flymachine.minecraftclanguage.content.logic.architecture.la64.util.BitMath;
import net.flymachine.minecraftclanguage.content.logic.cpu.la64.exception.LA64RuntimeException;

import java.util.Optional;
import java.util.function.Function;

public final class LA64Decoder {

    /**
     * 将机器码解码为指令对象，方法会自动根据已知指令的操作码长度进行匹配，返回最长匹配成功的指令对象
     *
     * @param machineCode 机器码
     * @return 解码后的指令对象
     * @throws LA64RuntimeException 如果机器码不匹配任何已知指令
     */
    public static LA64Instruction decode(int machineCode) {
        Optional<LA64InstructionInfo> optionalInfo = LA64InstructionSet.getByMachineCode(machineCode);
        if (optionalInfo.isEmpty()) {
            throw new LA64RuntimeException(LA64Exception.INE);
        }
        LA64InstructionInfo info = optionalInfo.get();

        LA64Operand[] ops;
        switch (info.format()) {
            case FORMAT_2RI12 -> ops = decode2RI12(info, machineCode);
            case FORMAT_2RI16 -> ops = decode2RI16(info, machineCode);
            case MISCELLANEOUS -> {
                Function<Integer, LA64Operand[]> decoder = info.decoder();
                if (decoder == null) {
                    throw new IllegalArgumentException(
                        "No decoder found for MISCELLANEOUS format instruction: " + info.format());
                }
                ops = decoder.apply(machineCode);
            }
            default -> throw new IllegalArgumentException("Unsupported instruction format: " + info.format());
        }
        return new LA64Instruction(info, ops);
    }

    private static LA64Operand[] decode2RI12(LA64InstructionInfo info, int machineCode) {
        // rd, rj, imm12(si12 / ui12)
        int rd = BitMath.getRd(machineCode);
        int rj = BitMath.getRj(machineCode);
        int imm12 = BitMath.extractBits(machineCode, 10, 12);
        return new LA64Operand[]{
            LA64Operand.gpr(rd), LA64Operand.gpr(rj), new LA64Operand(info.operandTypes()[2], imm12)};
    }

    private static LA64Operand[] decode2RI16(LA64InstructionInfo info, int machineCode) {
        // rd, rj, imm16
        int rd = BitMath.getRd(machineCode);
        int rj = BitMath.getRj(machineCode);
        int imm16 = BitMath.extractBits(machineCode, 10, 16);
        return new LA64Operand[]{
            LA64Operand.gpr(rd), LA64Operand.gpr(rj), new LA64Operand(info.operandTypes()[2], imm16)};
    }

}
