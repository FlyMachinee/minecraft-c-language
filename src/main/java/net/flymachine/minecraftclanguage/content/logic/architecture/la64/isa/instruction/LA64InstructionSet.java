package net.flymachine.minecraftclanguage.content.logic.architecture.la64.isa.instruction;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import net.flymachine.minecraftclanguage.content.logic.architecture.la64.isa.operand.LA64OperandType;
import net.flymachine.minecraftclanguage.content.logic.cpu.la64.LA64CpuState;

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
    public static Optional<LA64InstructionInfo> getByMnemonic(String mnemonic) {
        return Optional.ofNullable(BY_MNEMONIC.get(mnemonic));
    }

    /**
     * 检查字符串是否标识某条指令
     *
     * @param str 字符串
     * @return 如果字符串是某条指令的助记符，则返回 {@code true}，否则返回 {@code false}
     */
    public static boolean isMnemonic(String str) {
        return BY_MNEMONIC.containsKey(str);
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
    private static final Int2ObjectMap<LA64InstructionInfo> BY_OPCODE = new Int2ObjectOpenHashMap<>();

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
            (emulator, operands) -> {
                // addi.w rd, rj, si12
                /*
                    tmp = GR[rj][31:0] + SignExtend(si12, 32)
                    GR[rd] = SignExtend(tmp[31:0], GRLEN)
                 */
                LA64CpuState cpu = emulator.getCpuState();
                int rjValue = cpu.getGrWord(operands[1].value());
                int si12Value = operands[2].value();
                int temp = rjValue + si12Value;
                cpu.setGr(operands[0].value(), temp);
                cpu.pcNext();
            }));
        add(new LA64InstructionInfo(
            "jirl",
            0b0100_11,
            6,
            LA64InstructionFormat.FORMAT_2RI16,
            new LA64OperandType[]{LA64OperandType.REG, LA64OperandType.REG, LA64OperandType.OFFS16},
            null,
            null,
            (emulator, operands) -> {
                // jirl rd, rj, offs16
                /*
                    GR[rd] = PC + 4
                    PC = GR[rj] + SignExtend({offs16, 2'b0}, GRLEN)
                 */
                LA64CpuState cpu = emulator.getCpuState();
                cpu.setGr(operands[0].value(), cpu.getPc() + 4);
                long rjValue = cpu.getGr(operands[1].value());
                long offset = ((long) operands[2].value()) << 2;
                long nextPc = rjValue + offset;
                cpu.setPc(nextPc);
            }));
    }
}
