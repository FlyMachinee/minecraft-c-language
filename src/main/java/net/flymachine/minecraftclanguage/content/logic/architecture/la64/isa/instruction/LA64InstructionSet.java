package net.flymachine.minecraftclanguage.content.logic.architecture.la64.isa.instruction;

import net.flymachine.minecraftclanguage.content.logic.architecture.la64.isa.operand.LA64OperandType;

import java.util.*;

public final class LA64InstructionSet {

    private LA64InstructionSet() { }

    public static final List<LA64InstructionInfo> ALL_INSTRUCTIONS = new ArrayList<>();

    /**
     * 通过指令助记符来查询指令信息
     *
     * @param mnemonic 指令助记符
     * @return 指令信息
     */
    public static LA64InstructionInfo getByMnemonic(String mnemonic) {
        return BY_MNEMONIC.get(mnemonic);
    }

    /**
     * 通过指令操作码来查询指令信息
     *
     * @param opcode 指令操作码，必须是左对齐的，即已经左移到最高位
     * @return 指令信息
     */
    public static Optional<LA64InstructionInfo> getByOpcode(int opcode) {
        return Optional.ofNullable(BY_OPCODE.get(opcode));
    }

    /**
     * 通过机器码来查询指令信息，方法会自动根据已知指令的操作码长度进行匹配，返回最长匹配成功的指令信息
     *
     * @param machineCode 机器码
     * @return 最长匹配的指令信息，或 {@code null} 若该机器码不匹配任何已知指令
     */
    public static Optional<LA64InstructionInfo> getByMachineCode(int machineCode) {
        for (int len = maxOpcodeLength; len >= minOpcodeLength; len--) {
            int mask = 0xFFFFFFFF << (32 - len);
            Optional<LA64InstructionInfo> info = getByOpcode(machineCode & mask);
            if (info.isPresent()) {
                return info;
            }
        }
        return Optional.empty();
    }

    /**
     * 获取所有指令信息
     *
     * @return 指令信息列表
     */
    public static List<LA64InstructionInfo> getAllInstructions() {
        return ALL_INSTRUCTIONS;
    }

    private static final Map<String, LA64InstructionInfo> BY_MNEMONIC = new HashMap<>();
    private static final Map<Integer, LA64InstructionInfo> BY_OPCODE = new HashMap<>();

    private static int maxOpcodeLength = 0;
    private static int minOpcodeLength = 32;

    private static void add(LA64InstructionInfo info) {
        BY_MNEMONIC.put(info.mnemonic(), info);
        BY_OPCODE.put(info.opcode() << (32 - info.opcodeLength()), info);
        ALL_INSTRUCTIONS.add(info);
        maxOpcodeLength = Math.max(maxOpcodeLength, info.opcodeLength());
        minOpcodeLength = Math.min(minOpcodeLength, info.opcodeLength());
    }

    static {
        add(new LA64InstructionInfo(
            "addi.w",
            0b0000_0010_10,
            10,
            LA64InstructionFormat.FORMAT_2RI12,
            new LA64OperandType[]{LA64OperandType.REG, LA64OperandType.REG, LA64OperandType.SI12},
            null,
            null,
            (emulator, operands) -> { }));
        add(new LA64InstructionInfo(
            "jirl",
            0b0100_11,
            6,
            LA64InstructionFormat.FORMAT_2RI16,
            new LA64OperandType[]{LA64OperandType.REG, LA64OperandType.REG, LA64OperandType.OFFS16},
            null,
            null,
            (emulator, operands) -> { }));
    }
}
