package net.flymachine.minecraftclanguage.content.logic.architecture.la64.isa.instruction;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import net.flymachine.minecraftclanguage.content.logic.architecture.la64.isa.exception.LA64Exception;
import net.flymachine.minecraftclanguage.content.logic.architecture.la64.isa.operand.LA64Operand;
import net.flymachine.minecraftclanguage.content.logic.architecture.la64.isa.operand.LA64OperandType;
import net.flymachine.minecraftclanguage.content.logic.architecture.la64.util.BitMath;
import net.flymachine.minecraftclanguage.content.logic.cpu.la64.LA64CpuState;
import net.flymachine.minecraftclanguage.content.logic.cpu.la64.LA64MemoryManagementUnit;
import net.flymachine.minecraftclanguage.content.logic.cpu.la64.exception.LA64RuntimeException;
import net.flymachine.minecraftclanguage.content.logic.emulator.la64.LA64EmulatorHandler;
import net.flymachine.minecraftclanguage.content.logic.memory.MemoryLikeDevice;

import java.util.*;
import java.util.function.BiFunction;

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

    private final static LA64OperandType[] FORMAT_3GPR_OPTYPE
        = new LA64OperandType[]{LA64OperandType.GPR, LA64OperandType.GPR, LA64OperandType.GPR};
    private final static LA64OperandType[] FORMAT_2GPR_SI12_OPTYPE
        = new LA64OperandType[]{LA64OperandType.GPR, LA64OperandType.GPR, LA64OperandType.SI12};
    private final static LA64OperandType[] FORMAT_2GPR_UI12_OPTYPE
        = new LA64OperandType[]{LA64OperandType.GPR, LA64OperandType.GPR, LA64OperandType.UI12};
    private final static LA64OperandType[] FORMAT_2GPR_OFFS16_OPTYPE
        = new LA64OperandType[]{LA64OperandType.GPR, LA64OperandType.GPR, LA64OperandType.OFFS16};

    @FunctionalInterface
    private interface BinaryDoubleWordExecutor {
        void execute(
            LA64EmulatorHandler emulator, LA64Operand[] operands,
            BiFunction<Long, Long, Long> binaryFunction);
    }

    @FunctionalInterface
    private interface BinaryWordExecutor {
        void execute(
            LA64EmulatorHandler emulator, LA64Operand[] operands,
            BiFunction<Integer, Integer, Integer> binaryFunction);
    }

    private static final BinaryDoubleWordExecutor BINARY_DOUBLE_WORD_EXECUTOR =
        (emulator, operands, binaryFunction) -> {
            LA64CpuState cpu = emulator.getCpuState();
            long rjValue = cpu.getGr(operands[1].value());
            long rkValue = cpu.getGr(operands[2].value());
            long result = binaryFunction.apply(rjValue, rkValue);
            cpu.setGr(operands[0].value(), result);
            cpu.pcNext();
        };

    private static final BinaryWordExecutor BINARY_WORD_EXECUTOR =
        (emulator, operands, binaryFunction) -> {
            LA64CpuState cpu = emulator.getCpuState();
            int rjValue = cpu.getGrWord(operands[1].value());
            int rkValue = cpu.getGrWord(operands[2].value());
            int result = binaryFunction.apply(rjValue, rkValue);
            cpu.setGr(operands[0].value(), result);
            cpu.pcNext();
        };

    static {
        add(new LA64InstructionInfo(
            "sub.w",
            0b0000_0000_0001_0001_0,
            17,
            LA64InstructionFormat.FORMAT_3R,
            FORMAT_3GPR_OPTYPE,
            null,
            null,
            (emulator, operands) -> {
                // sub.w rd, rj, rk
                /*
                    tmp = GR[rj][31:0] - GR[rk][31:0]
                    GR[rd] = SignExtend(tmp[31:0], GRLEN)
                 */
                BINARY_WORD_EXECUTOR.execute(emulator, operands, (rj, rk) -> rj - rk);
            }));
        add(new LA64InstructionInfo(
            "nor",
            0b0000_0000_0001_0100_0,
            17,
            LA64InstructionFormat.FORMAT_3R,
            FORMAT_3GPR_OPTYPE,
            null,
            null,
            (emulator, operands) -> {
                // nor rd, rj, rk
                /*
                    GR[rd] = ~(GR[rj] | GR[rk])
                 */
                BINARY_DOUBLE_WORD_EXECUTOR.execute(emulator, operands, (rj, rk) -> ~(rj | rk));
            }));
        add(new LA64InstructionInfo(
            "or",
            0b0000_0000_0001_0101_0,
            17,
            LA64InstructionFormat.FORMAT_3R,
            FORMAT_3GPR_OPTYPE,
            null,
            null,
            (emulator, operands) -> {
                // or rd, rj, rk
                /*
                    GR[rd] = GR[rj] | GR[rk]
                 */
                BINARY_DOUBLE_WORD_EXECUTOR.execute(emulator, operands, (rj, rk) -> rj | rk);
            }));
        add(new LA64InstructionInfo(
            "addi.w",
            0b0000_0010_10,
            10,
            LA64InstructionFormat.FORMAT_2RI12,
            new LA64OperandType[]{LA64OperandType.GPR, LA64OperandType.GPR, LA64OperandType.SI12},
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
            "addi.d",
            0b0000_0010_11,
            10,
            LA64InstructionFormat.FORMAT_2RI12,
            FORMAT_2GPR_SI12_OPTYPE,
            null,
            null,
            (emulator, operands) -> {
                // addi.d rd, rj, si12
                /*
                    tmp = GR[rj][63:0] + SignExtend(si12, 64)
                    GR[rd] = tmp[63:0]
                 */
                LA64CpuState cpu = emulator.getCpuState();
                long rjValue = cpu.getGr(operands[1].value());
                long si12Value = operands[2].value();
                long temp = rjValue + si12Value;
                cpu.setGr(operands[0].value(), temp);
                cpu.pcNext();
            }));
        add(new LA64InstructionInfo(
            "ori",
            0b0000_0011_10,
            10,
            LA64InstructionFormat.FORMAT_2RI12,
            FORMAT_2GPR_UI12_OPTYPE,
            null,
            null,
            (emulator, operands) -> {
                // ori rd, rj, ui12
                /*
                    GR[rd] = GR[rj] | ZeroExtend(ui12, GRLEN)
                 */
                LA64CpuState cpu = emulator.getCpuState();
                long rjValue = cpu.getGr(operands[1].value());
                long ui12Value = operands[2].value() & 0xFFFL;
                cpu.setGr(operands[0].value(), rjValue | ui12Value);
                cpu.pcNext();
            }));
        add(new LA64InstructionInfo(
            "lu12i.w",
            0b0001_010,
            7,
            LA64InstructionFormat.MISCELLANEOUS,
            new LA64OperandType[]{LA64OperandType.GPR, LA64OperandType.SI20},
            (info, ops) -> {
                // rd, si20
                int rd = ops[0].value();
                int si20 = ops[1].value();
                return (info.opcode() << 25) | ((si20 & 0xFFFFF) << 5) | rd;
            },
            (machineCode) -> {
                // rd, si20
                int rd = BitMath.getRd(machineCode);
                int si20 = BitMath.extractSignedBits(machineCode, 5, 20);
                return new LA64Operand[]{
                    LA64Operand.gpr(rd), LA64Operand.si20(si20)
                };
            },
            (emulator, operands) -> {
                // lu12i.w rd, si20
                /*
                    GR[rd] = SignExtend({si20, 12'b0}, GRLEN)
                 */
                LA64CpuState cpu = emulator.getCpuState();
                cpu.setGr(operands[0].value(), ((long) operands[1].value()) << 12);
                cpu.pcNext();
            }));
        add(new LA64InstructionInfo(
            "ld.w",
            0b0010_1000_10,
            10,
            LA64InstructionFormat.FORMAT_2RI12,
            FORMAT_2GPR_SI12_OPTYPE,
            null,
            null,
            (emulator, operands) -> {
                // ld.w rd, rj, si12
                /*
                    vaddr = GR[rj] + SignExtend(si12, GRLEN)
                    AddressComplianceCheck(vaddr)
                    paddr = AddressTranslation(vaddr)
                    word = MemoryLoad(paddr, WORD)
                    GR[rd] = SignExtend(word, GRLEN)
                 */
                LA64CpuState cpu = emulator.getCpuState();
                MemoryLikeDevice ram = emulator.getRam();
                LA64MemoryManagementUnit mmu = emulator.getMemoryManagementUnit();

                long rjValue = cpu.getGr(operands[1].value());
                long offset = operands[2].value();
                long vaddr = rjValue + offset;
                if ((vaddr & 0b11) != 0) {
                    throw new LA64RuntimeException(LA64Exception.ALE);
                }
                long paddr = mmu.translateVirtualAddress(vaddr, LA64MemoryManagementUnit.LA64MemoryAccessType.LOAD);
                int word = ram.loadWord(paddr);
                cpu.setGr(operands[0].value(), word);
                cpu.pcNext();
            }));
        add(new LA64InstructionInfo(
            "ld.d",
            0b0010_1000_11,
            10,
            LA64InstructionFormat.FORMAT_2RI12,
            FORMAT_2GPR_SI12_OPTYPE,
            null,
            null,
            (emulator, operands) -> {
                // ld.d rd, rj, si12
                /*
                    vaddr = GR[rj] + SignExtend(si12, GRLEN)
                    AddressComplianceCheck(vaddr)
                    paddr = AddressTranslation(vaddr)
                    GR[rd] = MemoryLoad(paddr, DOUBLEWORD)
                 */
                LA64CpuState cpu = emulator.getCpuState();
                MemoryLikeDevice ram = emulator.getRam();
                LA64MemoryManagementUnit mmu = emulator.getMemoryManagementUnit();

                long rjValue = cpu.getGr(operands[1].value());
                long offset = operands[2].value();
                long vaddr = rjValue + offset;
                if ((vaddr & 0b111) != 0) {
                    throw new LA64RuntimeException(LA64Exception.ALE);
                }
                long paddr = mmu.translateVirtualAddress(vaddr, LA64MemoryManagementUnit.LA64MemoryAccessType.LOAD);
                cpu.setGr(operands[0].value(), ram.loadDoubleWord(paddr));
                cpu.pcNext();
            }));
        add(new LA64InstructionInfo(
            "st.w",
            0b0010_1001_10,
            10,
            LA64InstructionFormat.FORMAT_2RI12,
            FORMAT_2GPR_SI12_OPTYPE,
            null,
            null,
            (emulator, operands) -> {
                // st.w rd, rj, si12
                /*
                    vaddr = GR[rj] + SignExtend(si12, GRLEN)
                    AddressComplianceCheck(vaddr)
                    paddr = AddressTranslation(vaddr)
                    MemoryStore(GR[rd][31:0], paddr, WORD)
                 */
                LA64CpuState cpu = emulator.getCpuState();
                MemoryLikeDevice ram = emulator.getRam();
                LA64MemoryManagementUnit mmu = emulator.getMemoryManagementUnit();

                long rjValue = cpu.getGr(operands[1].value());
                long offset = operands[2].value();
                long vaddr = rjValue + offset;
                if ((vaddr & 0b11) != 0) {
                    throw new LA64RuntimeException(LA64Exception.ALE);
                }
                long paddr = mmu.translateVirtualAddress(vaddr, LA64MemoryManagementUnit.LA64MemoryAccessType.STORE);
                ram.storeWord(paddr, cpu.getGrWord(operands[0].value()));
                cpu.pcNext();
            }));
        add(new LA64InstructionInfo(
            "st.d",
            0b0010_1001_11,
            10,
            LA64InstructionFormat.FORMAT_2RI12,
            FORMAT_2GPR_SI12_OPTYPE,
            null,
            null,
            (emulator, operands) -> {
                // st.d rd, rj, si12
                /*
                    vaddr = GR[rj] + SignExtend(si12, GRLEN)
                    AddressComplianceCheck(vaddr)
                    paddr = AddressTranslation(vaddr)
                    MemoryStore(GR[rd][63:0], paddr, DOUBLEWORD)
                 */
                LA64CpuState cpu = emulator.getCpuState();
                MemoryLikeDevice ram = emulator.getRam();
                LA64MemoryManagementUnit mmu = emulator.getMemoryManagementUnit();

                long rjValue = cpu.getGr(operands[1].value());
                long offset = operands[2].value();
                long vaddr = rjValue + offset;
                if ((vaddr & 0b111) != 0) {
                    throw new LA64RuntimeException(LA64Exception.ALE);
                }
                long paddr = mmu.translateVirtualAddress(vaddr, LA64MemoryManagementUnit.LA64MemoryAccessType.STORE);
                ram.storeDoubleWord(paddr, cpu.getGr(operands[0].value()));
                cpu.pcNext();
            }));
        add(new LA64InstructionInfo(
            "jirl",
            0b0100_11,
            6,
            LA64InstructionFormat.FORMAT_2RI16,
            FORMAT_2GPR_OFFS16_OPTYPE,
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
                cpu.setPc(rjValue + offset);
            }));
    }
}
