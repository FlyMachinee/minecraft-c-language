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

    private static int lastLeftAlignedOpcode = 0;

    private static void add(LA64InstructionInfo info) {
        if (BY_MNEMONIC.containsKey(info.mnemonic())) {
            throw new IllegalArgumentException("Duplicate instruction mnemonic: " + info.mnemonic());
        }
        BY_MNEMONIC.put(info.mnemonic(), info);

        int leftAlignedOpcode = info.opcode() << (32 - info.opcodeLength());
        if (BY_OPCODE.containsKey(leftAlignedOpcode)) {
            throw new IllegalArgumentException("Duplicate instruction opcode: " + leftAlignedOpcode);
        }
        if (lastLeftAlignedOpcode > leftAlignedOpcode) {
            throw new IllegalArgumentException(
                "Opcode are not added in ascending order, current mnemonic: " + info.mnemonic() + ", last mnemonic: " +
                BY_OPCODE.get(lastLeftAlignedOpcode).mnemonic());
        }
        BY_OPCODE.put(leftAlignedOpcode, info);
        lastLeftAlignedOpcode = leftAlignedOpcode;

        ALL_INSTRUCTIONS.add(info);
        maxOpcodeLength = Math.max(maxOpcodeLength, info.opcodeLength());
        minOpcodeLength = Math.min(minOpcodeLength, info.opcodeLength());
    }

    @FunctionalInterface
    private interface BinaryDoubleWordExecutor {
        void execute(
            LA64EmulatorHandler emulator, LA64Operand[] operands,
            BiFunction<Long, Long, Long> binaryFunction);
    }

    @FunctionalInterface
    private interface BinaryDoubleWordBranchExecutor {
        void execute(
            LA64EmulatorHandler emulator, LA64Operand[] operands,
            BiFunction<Long, Long, Boolean> binaryFunction);
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

    private static final BinaryDoubleWordExecutor BINARY_DOUBLE_WORD_SI12_EXECUTOR =
        (emulator, operands, binaryFunction) -> {
            LA64CpuState cpu = emulator.getCpuState();
            long rjValue = cpu.getGr(operands[1].value());
            long si12Value = operands[2].value();
            long result = binaryFunction.apply(rjValue, si12Value);
            cpu.setGr(operands[0].value(), result);
            cpu.pcNext();
        };

    private static final BinaryDoubleWordExecutor BINARY_UI12_EXECUTOR =
        (emulator, operands, binaryFunction) -> {
            LA64CpuState cpu = emulator.getCpuState();
            long rjValue = cpu.getGr(operands[1].value());
            long ui12Value = operands[2].value() & 0xFFFL;
            long result = binaryFunction.apply(rjValue, ui12Value);
            cpu.setGr(operands[0].value(), result);
            cpu.pcNext();
        };

    private static final BinaryDoubleWordBranchExecutor BINARY_DOUBLE_WORD_BRANCH_EXECUTOR =
        (emulator, operands, binaryFunction) -> {
            // op rj, rd, offs
            LA64CpuState cpu = emulator.getCpuState();
            long rjValue = cpu.getGr(operands[0].value());
            long rdValue = cpu.getGr(operands[1].value());
            boolean branchTaken = binaryFunction.apply(rjValue, rdValue);
            if (branchTaken) {
                long offset = ((long) operands[2].value()) << 2;
                long target = cpu.getPc() + offset;
                cpu.setPc(target);
            } else {
                cpu.pcNext();
            }
        };

    static {
        add(LA64InstructionInfo.format3Gpr(
            "add.w",
            0b0000_0000_0001_0000_0,
            (emulator, operands) -> {
                // add.w rd, rj, rk
                /*
                    tmp = GR[rj][31:0] + GR[rk][31:0]
                    GR[rd] = SignExtend(tmp[31:0], GRLEN)
                 */
                BINARY_WORD_EXECUTOR.execute(emulator, operands, Integer::sum);
            }));
        add(LA64InstructionInfo.format3Gpr(
            "sub.w",
            0b0000_0000_0001_0001_0,
            (emulator, operands) -> {
                // sub.w rd, rj, rk
                /*
                    tmp = GR[rj][31:0] - GR[rk][31:0]
                    GR[rd] = SignExtend(tmp[31:0], GRLEN)
                 */
                BINARY_WORD_EXECUTOR.execute(emulator, operands, (rj, rk) -> rj - rk);
            }));
        add(LA64InstructionInfo.format3Gpr(
            "slt",
            0b0000_0000_0001_0010_0,
            (emulator, operands) -> {
                // slt rd, rj, rk
                /*
                    GR[rd] = (signed(GR[rj]) < signed(GR[rk])) ? 1 : 0
                 */
                BINARY_DOUBLE_WORD_EXECUTOR.execute(emulator, operands, (rj, rk) -> (rj < rk) ? 1L : 0L);
            }));
        add(LA64InstructionInfo.format3Gpr(
            "sltu",
            0b0000_0000_0001_0010_1,
            (emulator, operands) -> {
                // sltu rd, rj, rk
                /*
                    GR[rd] = (unsigned(GR[rj]) < unsigned(GR[rk])) ? 1 : 0
                 */
                BINARY_DOUBLE_WORD_EXECUTOR.execute(
                    emulator, operands,
                    (rj, rk) -> (Long.compareUnsigned(rj, rk) < 0) ? 1L : 0L);
            }));
        add(LA64InstructionInfo.format3Gpr(
            "nor",
            0b0000_0000_0001_0100_0,
            (emulator, operands) -> {
                // nor rd, rj, rk
                /*
                    GR[rd] = ~(GR[rj] | GR[rk])
                 */
                BINARY_DOUBLE_WORD_EXECUTOR.execute(emulator, operands, (rj, rk) -> ~(rj | rk));
            }));
        add(LA64InstructionInfo.format3Gpr(
            "and",
            0b0000_0000_0001_0100_1,
            (emulator, operands) -> {
                // and rd, rj, rk
                /*
                    GR[rd] = GR[rj] & GR[rk]
                 */
                BINARY_DOUBLE_WORD_EXECUTOR.execute(emulator, operands, (rj, rk) -> rj & rk);
            }));
        add(LA64InstructionInfo.format3Gpr(
            "or",
            0b0000_0000_0001_0101_0,
            (emulator, operands) -> {
                // or rd, rj, rk
                /*
                    GR[rd] = GR[rj] | GR[rk]
                 */
                BINARY_DOUBLE_WORD_EXECUTOR.execute(emulator, operands, (rj, rk) -> rj | rk);
            }));
        add(LA64InstructionInfo.format3Gpr(
            "xor",
            0b0000_0000_0001_0101_1,
            (emulator, operands) -> {
                // xor rd, rj, rk
                /*
                    GR[rd] = GR[rj] ^ GR[rk]
                 */
                BINARY_DOUBLE_WORD_EXECUTOR.execute(emulator, operands, (rj, rk) -> rj ^ rk);
            }));
        add(LA64InstructionInfo.format3Gpr(
            "sll.w",
            0b0000_0000_0001_0111_0,
            (emulator, operands) -> {
                // sll.w rd, rj, rk
                /*
                    tmp = SLL(GR[rj][31:0], GR[rk][4:0])
                    GR[rd] = SignExtend(tmp[31:0], GRLEN)
                 */
                BINARY_WORD_EXECUTOR.execute(emulator, operands, (rj, rk) -> rj << (rk & 0b11111));
            }));
        add(LA64InstructionInfo.format3Gpr(
            "sra.w",
            0b0000_0000_0001_1000_0,
            (emulator, operands) -> {
                // sra.w rd, rj, rk
                /*
                    tmp = SRA(GR[rj][31:0], GR[rk][4:0])
                    GR[rd] = SignExtend(tmp[31:0], GRLEN)
                 */
                BINARY_WORD_EXECUTOR.execute(emulator, operands, (rj, rk) -> rj >> (rk & 0b11111));
            }));
        add(LA64InstructionInfo.format3Gpr(
            "mul.w",
            0b0000_0000_0001_1100_0,
            (emulator, operands) -> {
                // mul.w rd, rj, rk
                /*
                    product = signed(GR[rj][31:0]) * signed(GR[rk][31:0])
                    GR[rd] = SignExtend(product[31:0], GRLEN)
                 */
                BINARY_WORD_EXECUTOR.execute(emulator, operands, (rj, rk) -> rj * rk);
            }));
        add(LA64InstructionInfo.format3Gpr(
            "div.w",
            0b0000_0000_0010_0000_0,
            (emulator, operands) -> {
                // div.w rd, rj, rk
                /*
                    quotient = signed(GR[rj][31:0]) / signed(GR[rk][31:0])
                    GR[rd] = SignExtend(quotient[31:0], GRLEN)
                 */
                BINARY_WORD_EXECUTOR.execute(emulator, operands, (rj, rk) -> rj / rk);
            }));
        add(LA64InstructionInfo.format3Gpr(
            "mod.w",
            0b0000_0000_0010_0000_1,
            (emulator, operands) -> {
                // mod.w rd, rj, rk
                /*
                    remainder = signed(GR[rj][31:0]) % signed(GR[rk][31:0])
                    GR[rd] = SignExtend(remainder[31:0], GRLEN)
                 */
                BINARY_WORD_EXECUTOR.execute(emulator, operands, (rj, rk) -> rj % rk);
            }));
        add(new LA64InstructionInfo(
            "slli.w",
            0b0000_0000_0100_0000_1,
            17,
            LA64InstructionFormat.FORMAT_3R,
            LA64OperandType.FORMAT_2GPR_UI5_OPTYPE,
            (emulator, operands) -> {
                // slli.w rd, rj, ui5
                /*
                    tmp = SLL(GR[rj][31:0], ui5)
                    GR[rd] = SignExtend(tmp[31:0], GRLEN)
                 */
                LA64CpuState cpuState = emulator.getCpuState();
                int rjValue = cpuState.getGrWord(operands[1].value());
                int ui5Value = operands[2].value();
                int temp = rjValue << ui5Value;
                cpuState.setGr(operands[0].value(), temp);
                cpuState.pcNext();
            }));
        add(new LA64InstructionInfo(
            "srai.w",
            0b0000_0000_0100_1000_1,
            17,
            LA64InstructionFormat.FORMAT_3R,
            LA64OperandType.FORMAT_2GPR_UI5_OPTYPE,
            (emulator, operands) -> {
                // srai.w rd, rj, ui5
                /*
                    tmp = SRA(GR[rj][31:0], ui5)
                    GR[rd] = SignExtend(tmp[31:0], GRLEN)
                 */
                LA64CpuState cpuState = emulator.getCpuState();
                int rjValue = cpuState.getGrWord(operands[1].value());
                int ui5Value = operands[2].value();
                int temp = rjValue >> ui5Value;
                cpuState.setGr(operands[0].value(), temp);
                cpuState.pcNext();
            }));
        add(LA64InstructionInfo.format2GprSi12(
            "slti",
            0b0000_0010_00,
            (emulator, operands) -> {
                // slti rd, rj, si12
                /*
                    tmp = SignExtend(si12, GRLEN)
                    GR[rd] = (signed(GR[rj]) < signed(tmp)) ? 1 : 0
                 */
                BINARY_DOUBLE_WORD_SI12_EXECUTOR.execute(emulator, operands, (rj, si12) -> rj < si12 ? 1L : 0L);
            }));
        add(LA64InstructionInfo.format2GprSi12(
            "sltui",
            0b0000_0010_01,
            (emulator, operands) -> {
                // sltui rd, rj, si12
                /*
                    tmp = SignExtend(si12, GRLEN)
                    GR[rd] = (unsigned(GR[rj]) < unsigned(tmp)) ? 1 : 0
                 */
                BINARY_DOUBLE_WORD_SI12_EXECUTOR.execute(
                    emulator, operands,
                    (rj, si12) -> Long.compareUnsigned(rj, si12) < 0 ? 1L : 0L);
            }));
        add(LA64InstructionInfo.format2GprSi12(
            "addi.w",
            0b0000_0010_10,
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
        add(LA64InstructionInfo.format2GprSi12(
            "addi.d",
            0b0000_0010_11,
            (emulator, operands) -> {
                // addi.d rd, rj, si12
                /*
                    tmp = GR[rj][63:0] + SignExtend(si12, 64)
                    GR[rd] = tmp[63:0]
                 */
                BINARY_DOUBLE_WORD_SI12_EXECUTOR.execute(emulator, operands, Long::sum);
            }));
        add(LA64InstructionInfo.format2GprUi12(
            "andi",
            0b0000_0011_01,
            (emulator, operands) -> {
                // andi rd, rj, ui12
                /*
                    GR[rd] = GR[rj] & ZeroExtend(ui12, GRLEN)
                 */
                BINARY_UI12_EXECUTOR.execute(emulator, operands, (rj, ui12) -> rj & ui12);
            }));
        add(LA64InstructionInfo.format2GprUi12(
            "ori",
            0b0000_0011_10,
            (emulator, operands) -> {
                // ori rd, rj, ui12
                /*
                    GR[rd] = GR[rj] | ZeroExtend(ui12, GRLEN)
                 */
                BINARY_UI12_EXECUTOR.execute(emulator, operands, (rj, ui12) -> rj | ui12);
            }));
        add(LA64InstructionInfo.format2GprUi12(
            "xori",
            0b0000_0011_11,
            (emulator, operands) -> {
                // xori rd, rj, ui12
                /*
                    GR[rd] = GR[rj] ^ ZeroExtend(ui12, GRLEN)
                 */
                BINARY_UI12_EXECUTOR.execute(emulator, operands, (rj, ui12) -> rj ^ ui12);
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
        add(LA64InstructionInfo.format2GprSi12(
            "ld.w",
            0b0010_1000_10,
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
        add(LA64InstructionInfo.format2GprSi12(
            "ld.d",
            0b0010_1000_11,
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
        add(LA64InstructionInfo.format2GprSi12(
            "st.w",
            0b0010_1001_10,
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
        add(LA64InstructionInfo.format2GprSi12(
            "st.d",
            0b0010_1001_11,
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
        add(LA64InstructionInfo.format1GPROffs21(
            "beqz",
            0b0100_00,
            (emulator, operands) -> {
                // beqz rj, offs21
                /*
                    if GR[rj]==0 :
                        PC = PC + SignExtend({offs21, 2'b0}, GRLEN)
                 */
                LA64CpuState cpu = emulator.getCpuState();
                long rjValue = cpu.getGr(operands[0].value());
                if (rjValue == 0) {
                    long offset = ((long) operands[1].value()) << 2;
                    long target = cpu.getPc() + offset;
                    cpu.setPc(target);
                } else {
                    cpu.pcNext();
                }
            }));
        add(LA64InstructionInfo.format1GPROffs21(
            "bnez",
            0b0100_01,
            (emulator, operands) -> {
                // bnez rj, offs21
                /*
                    if GR[rj]!=0 :
                        PC = PC + SignExtend({offs21, 2'b0}, GRLEN)
                 */
                LA64CpuState cpu = emulator.getCpuState();
                long rjValue = cpu.getGr(operands[0].value());
                if (rjValue != 0) {
                    long offset = ((long) operands[1].value()) << 2;
                    long target = cpu.getPc() + offset;
                    cpu.setPc(target);
                } else {
                    cpu.pcNext();
                }
            }));
        add(LA64InstructionInfo.format2GPROffs16(
            "jirl",
            0b0100_11,
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
        add(LA64InstructionInfo.formatOffs26(
            "b",
            0b0101_00,
            (emulator, operands) -> {
                // b offs26
                /*
                    PC = PC + SignExtend({offs26, 2'b0}, GRLEN)
                 */
                LA64CpuState cpu = emulator.getCpuState();
                long offset = ((long) operands[0].value()) << 2;
                long target = cpu.getPc() + offset;
                cpu.setPc(target);
            }));
        add(LA64InstructionInfo.formatOffs26(
            "bl",
            0b0101_01,
            (emulator, operands) -> {
                // bl offs26
                /*
                    GR[1] = PC + 4
                    PC = PC + SignExtend({offs26, 2'b0}, GRLEN)
                 */
                LA64CpuState cpu = emulator.getCpuState();
                cpu.setGr(1, cpu.getPc() + 4);
                long offset = ((long) operands[0].value()) << 2;
                long target = cpu.getPc() + offset;
                cpu.setPc(target);
            }));
        add(LA64InstructionInfo.format2GPROffs16(
            "beq",
            0b0101_10,
            (emulator, operands) -> {
                // beq rj, rd, offs16
                /*
                    if GR[rj]==GR[rd] :
                        PC = PC + SignExtend({offs16, 2'b0}, GRLEN)
                 */
                BINARY_DOUBLE_WORD_BRANCH_EXECUTOR.execute(emulator, operands, Objects::equals);
            }));
        add(LA64InstructionInfo.format2GPROffs16(
            "bne",
            0b0101_11,
            (emulator, operands) -> {
                // bne rj, rd, offs16
                /*
                    if GR[rj]!=GR[rd] :
                        PC = PC + SignExtend({offs16, 2'b0}, GRLEN)
                 */
                BINARY_DOUBLE_WORD_BRANCH_EXECUTOR.execute(emulator, operands, (rj, rd) -> !Objects.equals(rj, rd));
            }));
        add(LA64InstructionInfo.format2GPROffs16(
            "blt",
            0b0110_00,
            (emulator, operands) -> {
                // blt rj, rd, offs16
                /*
                    if signed(GR[rj]) < signed(GR[rd]) :
                        PC = PC + SignExtend({offs16, 2'b0}, GRLEN)
                 */
                BINARY_DOUBLE_WORD_BRANCH_EXECUTOR.execute(emulator, operands, (rj, rd) -> rj < rd);
            }));
        add(LA64InstructionInfo.format2GPROffs16(
            "bge",
            0b0110_01,
            (emulator, operands) -> {
                // bge rj, rd, offs16
                /*
                    if signed(GR[rj]) >= signed(GR[rd]) :
                        PC = PC + SignExtend({offs16, 2'b0}, GRLEN)
                 */
                BINARY_DOUBLE_WORD_BRANCH_EXECUTOR.execute(emulator, operands, (rj, rd) -> rj >= rd);
            }));
    }
}
