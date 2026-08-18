package net.flymachine.minecraftclanguage.content.logic.architecture.la64.isa.instruction;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import net.flymachine.minecraftclanguage.content.logic.architecture.la64.isa.exception.LA64Exception;
import net.flymachine.minecraftclanguage.content.logic.architecture.la64.isa.operand.LA64Operand;
import net.flymachine.minecraftclanguage.content.logic.architecture.la64.isa.operand.LA64OperandType;
import net.flymachine.minecraftclanguage.content.logic.architecture.la64.util.BitMath;
import net.flymachine.minecraftclanguage.content.logic.cpu.la64.LA64CpuState;
import net.flymachine.minecraftclanguage.content.logic.cpu.la64.LA64FpuState;
import net.flymachine.minecraftclanguage.content.logic.cpu.la64.LA64MemoryManagementUnit;
import net.flymachine.minecraftclanguage.content.logic.cpu.la64.exception.LA64RuntimeException;
import net.flymachine.minecraftclanguage.content.logic.emulator.la64.LA64EmulatorHandler;
import net.flymachine.minecraftclanguage.content.logic.memory.MemoryLikeDevice;
import org.apache.commons.lang3.function.TriFunction;

import java.util.*;
import java.util.function.BiFunction;
import java.util.function.Function;

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
     * @param opcode 指令操作码
     * @return 指令信息
     */
    public static Optional<LA64InstructionInfo> getByOpcode(int opcode) {
        return Optional.ofNullable(BY_OPCODE.get(opcode));
    }

    /**
     * 通过机器码来查询指令信息，方法会根据已知指令的操作码进行匹配
     *
     * @param machineCode 机器码
     * @return 匹配的指令信息，或 {@code null} 若该机器码不匹配任何已知指令
     */
    public static Optional<LA64InstructionInfo> getByMachineCode(int machineCode) {
        for (int mask : MASK_SET) {
            int opcode = machineCode & mask;
            Optional<LA64InstructionInfo> info = getByOpcode(opcode);
            if (info.isPresent() && info.get().mask() == mask) {
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
    // mask 最高位始终是1 所以是负数 使用降序排列
    private static final Set<Integer> MASK_SET = new TreeSet<>(Collections.reverseOrder());

    private static int lastOpcode = 0;

    private static void add(LA64InstructionInfo info) {
        if ((info.opcode() & info.mask()) != info.opcode()) {
            throw new IllegalArgumentException("Instruction mask cannot cover its opcode");
        }

        if (BY_MNEMONIC.containsKey(info.mnemonic())) {
            throw new IllegalArgumentException("Duplicate instruction mnemonic: " + info.mnemonic());
        }
        BY_MNEMONIC.put(info.mnemonic(), info);

        if (BY_OPCODE.containsKey(info.opcode())) {
            throw new IllegalArgumentException("Duplicate instruction opcode: " + info.opcode());
        }
        BY_OPCODE.put(info.opcode(), info);

        MASK_SET.add(info.mask());

        if (Integer.compareUnsigned(lastOpcode, info.opcode()) > 0) {
            throw new IllegalArgumentException(
                "Opcode are not added in ascending order, current mnemonic: " + info.mnemonic() + ", last mnemonic: " +
                BY_OPCODE.get(lastOpcode).mnemonic());
        }
        lastOpcode = info.opcode();

        ALL_INSTRUCTIONS.add(info);
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

    @FunctionalInterface
    private interface BinaryDoubleExecutor {
        void execute(
            LA64EmulatorHandler emulator, LA64Operand[] operands,
            BiFunction<Double, Double, Double> binaryFunction);
    }

    @FunctionalInterface
    private interface UnaryDoubleExecutor {
        void execute(
            LA64EmulatorHandler emulator, LA64Operand[] operands,
            Function<Double, Double> unaryFunction);
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

    private static final BinaryDoubleExecutor BINARY_DOUBLE_EXECUTOR =
        (emulator, operands, binaryFunction) -> {
            LA64CpuState cpu = emulator.getCpuState();
            LA64FpuState fpu = emulator.getFpuState();
            double fjValue = fpu.getFr(operands[1].value());
            double fkValue = fpu.getFr(operands[2].value());
            double result = binaryFunction.apply(fjValue, fkValue);
            fpu.setFr(operands[0].value(), result);
            cpu.pcNext();
        };

    private static final UnaryDoubleExecutor UNARY_DOUBLE_EXECUTOR =
        (emulator, operands, unaryFunction) -> {
            LA64CpuState cpu = emulator.getCpuState();
            LA64FpuState fpu = emulator.getFpuState();
            double fjValue = fpu.getFr(operands[1].value());
            double result = unaryFunction.apply(fjValue);
            fpu.setFr(operands[0].value(), result);
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
            "add.d",
            0b0000_0000_0001_0000_1,
            (emulator, operands) -> {
                // add.d rd, rj, rk
                /*
                    tmp = GR[rj][63:0] + GR[rk][63:0]
                    GR[rd] = tmp[63:0]
                 */
                BINARY_DOUBLE_WORD_EXECUTOR.execute(emulator, operands, Long::sum);
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
            "sub.d",
            0b0000_0000_0001_0001_1,
            (emulator, operands) -> {
                // sub.d rd, rj, rk
                /*
                    tmp = GR[rj][63:0] - GR[rk][63:0]
                    GR[rd] = tmp[63:0]
                 */
                BINARY_DOUBLE_WORD_EXECUTOR.execute(emulator, operands, (rj, rk) -> rj - rk);
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
                BINARY_WORD_EXECUTOR.execute(emulator, operands, (rj, rk) -> rj << rk);
            }));
        add(LA64InstructionInfo.format3Gpr(
            "srl.w",
            0b0000_0000_0001_0111_1,
            (emulator, operands) -> {
                // srl.w rd, rj, rk
                /*
                    tmp = SRL(GR[rj][31:0], GR[rk][4:0])
                    GR[rd] = SignExtend(tmp[31:0], GRLEN)
                 */
                BINARY_WORD_EXECUTOR.execute(emulator, operands, (rj, rk) -> rj >>> rk);
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
                BINARY_WORD_EXECUTOR.execute(emulator, operands, (rj, rk) -> rj >> rk);
            }));
        add(LA64InstructionInfo.format3Gpr(
            "sll.d",
            0b0000_0000_0001_1000_1,
            (emulator, operands) -> {
                // sll.d rd, rj, rk
                /*
                    GR[rd] = SLL(GR[rj][63:0], GR[rk][5:0])
                 */
                BINARY_DOUBLE_WORD_EXECUTOR.execute(emulator, operands, (rj, rk) -> rj << rk);
            }));
        add(LA64InstructionInfo.format3Gpr(
            "srl.d",
            0b0000_0000_0001_1001_0,
            (emulator, operands) -> {
                // srl.d rd, rj, rk
                /*
                    GR[rd] = SRL(GR[rj][63:0], GR[rk][5:0])
                 */
                BINARY_DOUBLE_WORD_EXECUTOR.execute(emulator, operands, (rj, rk) -> rj >>> rk);
            }));
        add(LA64InstructionInfo.format3Gpr(
            "sra.d",
            0b0000_0000_0001_1001_1,
            (emulator, operands) -> {
                // sra.d rd, rj, rk
                /*
                    GR[rd] = SRA(GR[rj][63:0], GR[rk][5:0])
                 */
                BINARY_DOUBLE_WORD_EXECUTOR.execute(emulator, operands, (rj, rk) -> rj >> rk);
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
            "mul.d",
            0b0000_0000_0001_1101_1,
            (emulator, operands) -> {
                // mul.d rd, rj, rk
                /*
                    product = signed(GR[rj][63:0]) * signed(GR[rk][63:0])
                    GR[rd] = product[63:0]
                 */
                BINARY_DOUBLE_WORD_EXECUTOR.execute(emulator, operands, (rj, rk) -> rj * rk);
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
        add(LA64InstructionInfo.format3Gpr(
            "div.wu",
            0b0000_0000_0010_0001_0,
            (emulator, operands) -> {
                // div.wu rd, rj, rk
                /*
                    quotient = unsigned(GR[rj][31:0]) / unsigned(GR[rk][31:0])
                    GR[rd] = SignExtend(quotient[31:0], GRLEN)
                 */
                BINARY_WORD_EXECUTOR.execute(emulator, operands, Integer::divideUnsigned);
            }));
        add(LA64InstructionInfo.format3Gpr(
            "mod.wu",
            0b0000_0000_0010_0001_1,
            (emulator, operands) -> {
                // mod.wu rd, rj, rk
                /*
                    remainder = unsigned(GR[rj][31:0]) % unsigned(GR[rk][31:0])
                    GR[rd] = SignExtend(remainder[31:0], GRLEN)
                 */
                BINARY_WORD_EXECUTOR.execute(emulator, operands, Integer::remainderUnsigned);
            }));
        add(LA64InstructionInfo.format3Gpr(
            "div.d",
            0b0000_0000_0010_0010_0,
            (emulator, operands) -> {
                // div.d rd, rj, rk
                /*
                    GR[rd] = signed(GR[rj][63:0]) / signed(GR[rk][63:0])
                 */
                BINARY_DOUBLE_WORD_EXECUTOR.execute(emulator, operands, (rj, rk) -> rj / rk);
            }));
        add(LA64InstructionInfo.format3Gpr(
            "mod.d",
            0b0000_0000_0010_0010_1,
            (emulator, operands) -> {
                // mod.d rd, rj, rk
                /*
                    GR[rd] = signed(GR[rj][63:0]) % signed(GR[rk][63:0])
                 */
                BINARY_DOUBLE_WORD_EXECUTOR.execute(emulator, operands, (rj, rk) -> rj % rk);
            }));
        add(LA64InstructionInfo.format3Gpr(
            "div.du",
            0b0000_0000_0010_0011_0,
            (emulator, operands) -> {
                // div.du rd, rj, rk
                /*
                    GR[rd] = unsigned(GR[rj][63:0]) / unsigned(GR[rk][63:0])
                 */
                BINARY_DOUBLE_WORD_EXECUTOR.execute(emulator, operands, Long::divideUnsigned);
            }));
        add(LA64InstructionInfo.format3Gpr(
            "mod.du",
            0b0000_0000_0010_0011_1,
            (emulator, operands) -> {
                // mod.du rd, rj, rk
                /*
                    GR[rd] = unsigned(GR[rj][63:0]) % unsigned(GR[rk][63:0])
                 */
                BINARY_DOUBLE_WORD_EXECUTOR.execute(emulator, operands, Long::remainderUnsigned);
            }));
        add(new LA64InstructionInfo(
            "slli.w",
            0b0000_0000_0100_0000_1 << 15,
            ((1 << 17) - 1) << 15,
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
            "slli.d",
            0b0000_0000_0100_0001 << 16,
            ((1 << 16) - 1) << 16,
            LA64InstructionFormat.MISCELLANEOUS,
            new LA64OperandType[]{LA64OperandType.GPR, LA64OperandType.GPR, LA64OperandType.UI6},
            (info, ops) -> {
                // rd, rj, ui6
                int rd = ops[0].value();
                int rj = ops[1].value();
                int ui6 = ops[2].value();
                return info.opcode() | (ui6 << 10) | (rj << 5) | rd;
            },
            (machineCode) -> {
                // rd, rj, ui6
                int rd = BitMath.getRd(machineCode);
                int rj = BitMath.getRj(machineCode);
                int ui6 = BitMath.extractBits(machineCode, 10, 6);
                return new LA64Operand[]{
                    LA64Operand.gpr(rd), LA64Operand.gpr(rj), LA64Operand.ui6(ui6)
                };
            },
            (emulator, operands) -> {
                // slli.d rd, rj, ui6
                /*
                    GR[rd] = SLL(GR[rj][63:0], ui6)
                 */
                LA64CpuState cpuState = emulator.getCpuState();
                long rjValue = cpuState.getGr(operands[1].value());
                int ui6Value = operands[2].value();
                cpuState.setGr(operands[0].value(), rjValue << ui6Value);
                cpuState.pcNext();
            }));
        add(new LA64InstructionInfo(
            "srli.w",
            0b0000_0000_0100_0100_1 << 15,
            ((1 << 17) - 1) << 15,
            LA64InstructionFormat.FORMAT_3R,
            LA64OperandType.FORMAT_2GPR_UI5_OPTYPE,
            (emulator, operands) -> {
                // srli.w rd, rj, ui5
                /*
                    tmp = SRL(GR[rj][31:0], ui5)
                    GR[rd] = SignExtend(tmp[31:0], GRLEN)
                 */
                LA64CpuState cpuState = emulator.getCpuState();
                int rjValue = cpuState.getGrWord(operands[1].value());
                int ui5Value = operands[2].value();
                int temp = rjValue >>> ui5Value;
                cpuState.setGr(operands[0].value(), temp);
                cpuState.pcNext();
            }));
        add(new LA64InstructionInfo(
            "srli.d",
            0b0000_0000_0100_0101 << 16,
            ((1 << 16) - 1) << 16,
            LA64InstructionFormat.MISCELLANEOUS,
            new LA64OperandType[]{LA64OperandType.GPR, LA64OperandType.GPR, LA64OperandType.UI6},
            (info, ops) -> {
                // rd, rj, ui6
                int rd = ops[0].value();
                int rj = ops[1].value();
                int ui6 = ops[2].value();
                return info.opcode() | (ui6 << 10) | (rj << 5) | rd;
            },
            (machineCode) -> {
                // rd, rj, ui6
                int rd = BitMath.getRd(machineCode);
                int rj = BitMath.getRj(machineCode);
                int ui6 = BitMath.extractBits(machineCode, 10, 6);
                return new LA64Operand[]{
                    LA64Operand.gpr(rd), LA64Operand.gpr(rj), LA64Operand.ui6(ui6)
                };
            },
            (emulator, operands) -> {
                // srli.d rd, rj, ui6
                /*
                    GR[rd] = SRL(GR[rj][63:0], ui6)
                 */
                LA64CpuState cpuState = emulator.getCpuState();
                long rjValue = cpuState.getGr(operands[1].value());
                int ui6Value = operands[2].value();
                cpuState.setGr(operands[0].value(), rjValue >>> ui6Value);
                cpuState.pcNext();
            }));
        add(new LA64InstructionInfo(
            "srai.w",
            0b0000_0000_0100_1000_1 << 15,
            ((1 << 17) - 1) << 15,
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
        add(new LA64InstructionInfo(
            "srai.d",
            0b0000_0000_0100_1001 << 16,
            ((1 << 16) - 1) << 16,
            LA64InstructionFormat.MISCELLANEOUS,
            new LA64OperandType[]{LA64OperandType.GPR, LA64OperandType.GPR, LA64OperandType.UI6},
            (info, ops) -> {
                // rd, rj, ui6
                int rd = ops[0].value();
                int rj = ops[1].value();
                int ui6 = ops[2].value();
                return info.opcode() | (ui6 << 10) | (rj << 5) | rd;
            },
            (machineCode) -> {
                // rd, rj, ui6
                int rd = BitMath.getRd(machineCode);
                int rj = BitMath.getRj(machineCode);
                int ui6 = BitMath.extractBits(machineCode, 10, 6);
                return new LA64Operand[]{
                    LA64Operand.gpr(rd), LA64Operand.gpr(rj), LA64Operand.ui6(ui6)
                };
            },
            (emulator, operands) -> {
                // srai.d rd, rj, ui6
                /*
                    GR[rd] = SRA(GR[rj][63:0], ui6)
                 */
                LA64CpuState cpuState = emulator.getCpuState();
                long rjValue = cpuState.getGr(operands[1].value());
                int ui6Value = operands[2].value();
                cpuState.setGr(operands[0].value(), rjValue >> ui6Value);
                cpuState.pcNext();
            }));
        add(new LA64InstructionInfo(
            "bstrpick.d",
            0b0000_0000_11 << 22,
            ((1 << 10) - 1) << 22,
            LA64InstructionFormat.MISCELLANEOUS,
            new LA64OperandType[]{LA64OperandType.GPR, LA64OperandType.GPR, LA64OperandType.UI6, LA64OperandType.UI6},
            (info, ops) -> {
                // rd, rj, msbd(ui6), lsbd(ui6)
                int rd = ops[0].value();
                int rj = ops[1].value();
                int msbd = ops[2].value();
                int lsbd = ops[3].value();
                return info.opcode() | (msbd << 16) | (lsbd << 10) | (rj << 5) | rd;
            },
            (machineCode) -> {
                // rd, rj, msbd(ui6), lsbd(ui6)
                int rd = BitMath.getRd(machineCode);
                int rj = BitMath.getRj(machineCode);
                int msbd = BitMath.extractBits(machineCode, 16, 6);
                int lsbd = BitMath.extractBits(machineCode, 10, 6);
                return new LA64Operand[]{
                    LA64Operand.gpr(rd), LA64Operand.gpr(rj), LA64Operand.ui6(msbd), LA64Operand.ui6(lsbd)
                };
            },
            (emulator, operands) -> {
                // bstrpick.d rd, rj, msbd, lsbd
                /*
                    GR[rd] = ZeroExtend(GR[rj][msbd:lsbd], 64)
                 */
                LA64CpuState cpuState = emulator.getCpuState();
                long rjValue = cpuState.getGr(operands[1].value());
                int msbdValue = operands[2].value();
                int lsbdValue = operands[3].value();
                cpuState.setGr(operands[0].value(), BitMath.extractBits(rjValue, lsbdValue, msbdValue - lsbdValue + 1));
                cpuState.pcNext();
            }));
        add(LA64InstructionInfo.format3Fpr(
            "fadd.d",
            0b0000_0001_0000_0001_0,
            (emulator, operands) -> {
                // fadd.d fd, fj, fk
                /*
                    FR[fd] = FP64_addition(FR[fj], FR[fk])
                 */
                BINARY_DOUBLE_EXECUTOR.execute(emulator, operands, Double::sum);
            }));
        add(LA64InstructionInfo.format3Fpr(
            "fsub.d",
            0b0000_0001_0000_0011_0,
            (emulator, operands) -> {
                // fsub.d fd, fj, fk
                /*
                    FR[fd] = FP64_subtraction(FR[fj], FR[fk])
                 */
                BINARY_DOUBLE_EXECUTOR.execute(emulator, operands, (a, b) -> a - b);
            }));
        add(LA64InstructionInfo.format3Fpr(
            "fmul.d",
            0b0000_0001_0000_0101_0,
            (emulator, operands) -> {
                // fmul.d fd, fj, fk
                /*
                    FR[fd] = FP64_multiplication(FR[fj], FR[fk])
                 */
                BINARY_DOUBLE_EXECUTOR.execute(emulator, operands, (a, b) -> a * b);
            }));
        add(LA64InstructionInfo.format3Fpr(
            "fdiv.d",
            0b0000_0001_0000_0111_0,
            (emulator, operands) -> {
                // fdiv.d fd, fj, fk
                /*
                    FR[fd] = FP64_division(FR[fj], FR[fk])
                 */
                BINARY_DOUBLE_EXECUTOR.execute(emulator, operands, (a, b) -> a / b);
            }));
        add(LA64InstructionInfo.format2Fpr(
            "fneg.d",
            0b0000_0001_0001_0100_0001_10,
            (emulator, operands) -> {
                // fneg.d fd, fj
                /*
                    FR[fd] = FP64_negate(FR[fj])
                 */
                UNARY_DOUBLE_EXECUTOR.execute(emulator, operands, (fj) -> -fj);
            }));
        add(LA64InstructionInfo.format2Fpr(
            "fmov.d",
            0b0000_0001_0001_0100_1001_10,
            (emulator, operands) -> {
                // fmov.d fd, fj
                /*
                    FR[fd] = FR[fj]
                 */
                UNARY_DOUBLE_EXECUTOR.execute(emulator, operands, (fj) -> fj);
            }));
        add(new LA64InstructionInfo(
            "movgr2fr.d",
            0b0000_0001_0001_0100_1010_10 << 10,
            ((1 << 22) - 1) << 10,
            LA64InstructionFormat.FORMAT_2R,
            new LA64OperandType[]{LA64OperandType.FPR, LA64OperandType.GPR},
            (info, ops) -> {
                // fd, rj
                int fd = ops[0].value();
                int rj = ops[1].value();
                return info.opcode() | rj << 5 | fd;
            },
            (machineCode) -> {
                // fd, rj
                int fd = BitMath.getRd(machineCode);
                int rj = BitMath.getRj(machineCode);
                return new LA64Operand[]{LA64Operand.fpr(fd), LA64Operand.gpr(rj)};
            },
            (emulator, operands) -> {
                // movgr2fr.d fd, rj
                /*
                    FR[fd] = GR[rj]
                 */
                LA64CpuState cpuState = emulator.getCpuState();
                LA64FpuState fpuState = emulator.getFpuState();
                fpuState.setFrL(operands[0].value(), cpuState.getGr(operands[1].value()));
                cpuState.pcNext();
            }));
        add(new LA64InstructionInfo(
            "movfr2gr.d",
            0b0000_0001_0001_0100_1011_10 << 10,
            ((1 << 22) - 1) << 10,
            LA64InstructionFormat.FORMAT_2R,
            new LA64OperandType[]{LA64OperandType.GPR, LA64OperandType.FPR},
            (info, ops) -> {
                // rd, fj
                int rd = ops[0].value();
                int fj = ops[1].value();
                return info.opcode() | fj << 5 | rd;
            },
            (machineCode) -> {
                // rd, fj
                int rd = BitMath.getRd(machineCode);
                int fj = BitMath.getRj(machineCode);
                return new LA64Operand[]{LA64Operand.gpr(rd), LA64Operand.fpr(fj)};
            },
            (emulator, operands) -> {
                // movfr2gr.d rd, fj
                /*
                    GR[rd] = FR[fj]
                 */
                LA64CpuState cpuState = emulator.getCpuState();
                LA64FpuState fpuState = emulator.getFpuState();
                cpuState.setGr(operands[0].value(), fpuState.getFrL(operands[1].value()));
                cpuState.pcNext();
            }));
        add(new LA64InstructionInfo(
            "movcf2gr",
            0b0000_0001_0001_0100_1101_1100 << 8,
            ((1 << 24) - 1) << 8,
            LA64InstructionFormat.MISCELLANEOUS,
            new LA64OperandType[]{LA64OperandType.GPR, LA64OperandType.CFR},
            (info, ops) -> {
                // rd, cj
                int rd = ops[0].value();
                int cj = ops[1].value();
                return info.opcode() | ((cj & 0b111) << 5) | rd;
            },
            (machineCode) -> {
                // rd, cj
                int rd = BitMath.getRd(machineCode);
                int cj = BitMath.extractBits(machineCode, 5, 3);
                return new LA64Operand[]{LA64Operand.gpr(rd), LA64Operand.cfr(cj)};
            },
            (emulator, operands) -> {
                // movcf2gr rd, cf
                /*
                    GR[rd] = ZeroExtend(CFR[cj], GRLEN)
                 */
                LA64CpuState cpuState = emulator.getCpuState();
                LA64FpuState fpuState = emulator.getFpuState();
                cpuState.setGr(operands[0].value(), fpuState.getCfr(operands[1].value()) ? 1 : 0);
                cpuState.pcNext();
            }
        ));
        add(LA64InstructionInfo.format2Fpr(
            "ftintrz.w.d",
            0b0000_0001_0001_1010_1000_10,
            (emulator, operands) -> {
                // ftintrz.w.d fd, fj
                /*
                    FR[fd] = FP64convertToSint32(FR[fj], 1)
                 */
                LA64CpuState cpuState = emulator.getCpuState();
                LA64FpuState fpuState = emulator.getFpuState();
                int int32 = (int) fpuState.getFr(operands[1].value());
                fpuState.setFrL(operands[0].value(), int32);
                cpuState.pcNext();
            }));
        add(LA64InstructionInfo.format2Fpr(
            "ftintrz.l.d",
            0b0000_0001_0001_1010_1010_10,
            (emulator, operands) -> {
                // ftintrz.l.d fd, fj
                /*
                    FR[fd] = FP64convertToSint64(FR[fj], 1)
                 */
                LA64CpuState cpuState = emulator.getCpuState();
                LA64FpuState fpuState = emulator.getFpuState();
                long int64 = (long) fpuState.getFr(operands[1].value());
                fpuState.setFrL(operands[0].value(), int64);
                cpuState.pcNext();
            }));
        add(LA64InstructionInfo.format2Fpr(
            "ffint.d.w",
            0b0000_0001_0001_1101_0010_00,
            (emulator, operands) -> {
                // ffint.d.w fd, fj
                /*
                    FR[fd] = FP64_convertFromInt(FR[fj][31:0], SINT32)
                 */
                LA64CpuState cpuState = emulator.getCpuState();
                LA64FpuState fpuState = emulator.getFpuState();
                int int32 = (int) fpuState.getFrL(operands[1].value());
                fpuState.setFr(operands[0].value(), int32);
                cpuState.pcNext();
            }));
        add(LA64InstructionInfo.format2Fpr(
            "ffint.d.l",
            0b0000_0001_0001_1101_0010_10,
            (emulator, operands) -> {
                // ffint.d.l fd, fj
                /*
                    FR[fd] = FP64_convertFromInt(FR[fj], SINT64)
                 */
                LA64CpuState cpuState = emulator.getCpuState();
                LA64FpuState fpuState = emulator.getFpuState();
                long int64 = fpuState.getFrL(operands[1].value());
                fpuState.setFr(operands[0].value(), int64);
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
        add(LA64InstructionInfo.format2GprSi12(
            "lu52i.d",
            0b0000_0011_00,
            (emulator, operands) -> {
                // lu52i.d rd, rj, si12
                /*
                    GR[rd] = {si12, GR[rj][51:0]}
                 */
                BINARY_DOUBLE_WORD_SI12_EXECUTOR.execute(
                    emulator, operands,
                    (rj, si12) -> ((si12 & 0xFFFL) << 52) | (rj & 0xFFFFFFFFFFFFFL));
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

        {
            int commonOpcode = 0b0000_1100_0010;
            // fcmp.cond.d cd, fj, fk

            TriFunction<Double, Double, Integer, Boolean> fcmpExecutorHelper = (fj, fk, cond) -> {
                boolean un = Double.isNaN(fj) || Double.isNaN(fk);
                boolean lt = !un && (fj < fk);
                boolean gt = !un && (fj > fk);
                boolean eq = !un && (fj.equals(fk));

                return switch (LA64FloatCompareCondition.fromCode(cond)) {
                    case CAF -> false;
                    case CUN -> un;
                    case CEQ -> eq;
                    case CUEQ -> un || eq;
                    case CLT -> lt;
                    case CULT -> un || lt;
                    case CLE -> lt || eq;
                    case CULE -> un || lt || eq;
                    case CNE -> gt || lt;
                    case COR -> gt || lt || eq;
                    case CUNE -> un || gt || lt;
                    default -> throw new UnsupportedOperationException("Unsupported condition: " + cond);
                };
            };

            LA64OperandType[] optypes = new LA64OperandType[]{
                LA64OperandType.CFR, LA64OperandType.FPR, LA64OperandType.FPR
            };

            for (LA64FloatCompareCondition cond : LA64FloatCompareCondition.values()) {
                String condName = cond.mnemonic();
                int code = cond.getCode();
                if (condName.charAt(0) == 'S') {
                    continue;
                }

                add(new LA64InstructionInfo(
                    "fcmp." + condName + ".d",
                    commonOpcode << 20 | code << 15,
                    0xFFFF8018,
                    LA64InstructionFormat.MISCELLANEOUS,
                    optypes,
                    (info, ops) -> {
                        // cd, fj, fk
                        int cd = ops[0].value();
                        int fj = ops[1].value();
                        int fk = ops[2].value();
                        return info.opcode() | fk << 10 | fj << 5 | (cd & 0b111);
                    },
                    (machineCode) -> {
                        // cd, fj, fk
                        int cd = BitMath.extractBits(machineCode, 3);
                        int fj = BitMath.getRj(machineCode);
                        int fk = BitMath.getRk(machineCode);
                        return new LA64Operand[]{
                            LA64Operand.cfr(cd), LA64Operand.fpr(fj), LA64Operand.fpr(fk)
                        };
                    },
                    (emulator, operands) -> {
                        // fcmp.cond.d cd, fj, fk
                        LA64CpuState cpu = emulator.getCpuState();
                        LA64FpuState fpu = emulator.getFpuState();
                        boolean ccRes = fcmpExecutorHelper.apply(
                            fpu.getFr(operands[1].value()),
                            fpu.getFr(operands[2].value()),
                            code
                        );
                        fpu.setCfr(operands[0].value(), ccRes);
                        cpu.pcNext();
                    }
                ));
            }
        }

        add(new LA64InstructionInfo(
            "lu12i.w",
            0b0001_010 << 25,
            ((1 << 7) - 1) << 25,
            LA64InstructionFormat.MISCELLANEOUS,
            new LA64OperandType[]{LA64OperandType.GPR, LA64OperandType.SI20},
            (info, ops) -> {
                // rd, si20
                int rd = ops[0].value();
                int si20 = ops[1].value();
                return info.opcode() | ((si20 & 0xFFFFF) << 5) | rd;
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
            "lu32i.d",
            0b0001_011 << 25,
            ((1 << 7) - 1) << 25,
            LA64InstructionFormat.MISCELLANEOUS,
            new LA64OperandType[]{LA64OperandType.GPR, LA64OperandType.SI20},
            (info, ops) -> {
                // rd, si20
                int rd = ops[0].value();
                int si20 = ops[1].value();
                return info.opcode() | ((si20 & 0xFFFFF) << 5) | rd;
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
                // lu32i.d rd, si20
                /*
                    GR[rd] = {SignExtend(si20, 32), GR[rd][31:0]}
                 */
                LA64CpuState cpu = emulator.getCpuState();
                int lower32 = cpu.getGrWord(operands[0].value());
                cpu.setGr(operands[0].value(), ((long) operands[1].value()) << 32 | (lower32 & 0xFFFFFFFFL));
                cpu.pcNext();
            }));
        add(new LA64InstructionInfo(
            "pcalau12i",
            0b0001_101 << 25,
            ((1 << 7) - 1) << 25,
            LA64InstructionFormat.MISCELLANEOUS,
            new LA64OperandType[]{LA64OperandType.GPR, LA64OperandType.SI20},
            (info, ops) -> {
                // rd, si20
                int rd = ops[0].value();
                int si20 = ops[1].value();
                return info.opcode() | ((si20 & 0xFFFFF) << 5) | rd;
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
                // pcalau12i rd, si20
                /*
                    tmp = PC + SignExtend({si20, 12'b0}, GRLEN)
                    GR[rd] = {tmp[GRLEN-1:12], 12'b0}
                 */
                LA64CpuState cpu = emulator.getCpuState();
                long tmp = cpu.getPc() + (((long) operands[1].value()) << 12);
                cpu.setGr(operands[0].value(), tmp & 0xFFFFFFFFFFFFF000L);
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
                MemoryLikeDevice memory = emulator.getMemory();
                LA64MemoryManagementUnit mmu = emulator.getMemoryManagementUnit();

                long rjValue = cpu.getGr(operands[1].value());
                long offset = operands[2].value();
                long vaddr = rjValue + offset;
                if ((vaddr & 0b11) != 0) {
                    throw new LA64RuntimeException(LA64Exception.ALE);
                }
                long paddr = mmu.translateVirtualAddress(vaddr, LA64MemoryManagementUnit.LA64MemoryAccessType.LOAD);
                int word = memory.loadWord(paddr);
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
                MemoryLikeDevice memory = emulator.getMemory();
                LA64MemoryManagementUnit mmu = emulator.getMemoryManagementUnit();

                long rjValue = cpu.getGr(operands[1].value());
                long offset = operands[2].value();
                long vaddr = rjValue + offset;
                if ((vaddr & 0b111) != 0) {
                    throw new LA64RuntimeException(LA64Exception.ALE);
                }
                long paddr = mmu.translateVirtualAddress(vaddr, LA64MemoryManagementUnit.LA64MemoryAccessType.LOAD);
                cpu.setGr(operands[0].value(), memory.loadDoubleWord(paddr));
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
                MemoryLikeDevice memory = emulator.getMemory();
                LA64MemoryManagementUnit mmu = emulator.getMemoryManagementUnit();

                long rjValue = cpu.getGr(operands[1].value());
                long offset = operands[2].value();
                long vaddr = rjValue + offset;
                if ((vaddr & 0b11) != 0) {
                    throw new LA64RuntimeException(LA64Exception.ALE);
                }
                long paddr = mmu.translateVirtualAddress(vaddr, LA64MemoryManagementUnit.LA64MemoryAccessType.STORE);
                memory.storeWord(paddr, cpu.getGrWord(operands[0].value()));
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
                MemoryLikeDevice memory = emulator.getMemory();
                LA64MemoryManagementUnit mmu = emulator.getMemoryManagementUnit();

                long rjValue = cpu.getGr(operands[1].value());
                long offset = operands[2].value();
                long vaddr = rjValue + offset;
                if ((vaddr & 0b111) != 0) {
                    throw new LA64RuntimeException(LA64Exception.ALE);
                }
                long paddr = mmu.translateVirtualAddress(vaddr, LA64MemoryManagementUnit.LA64MemoryAccessType.STORE);
                memory.storeDoubleWord(paddr, cpu.getGr(operands[0].value()));
                cpu.pcNext();
            }));

        add(LA64InstructionInfo.formatFprGprSi12(
            "fld.d",
            0b0010_1011_10,
            (emulator, operands) -> {
                // fld.d fd, rj, si12
                /*
                    vaddr = GR[rj] + SignExtend(si12, GRLEN)
                    AddressComplianceCheck(vaddr)
                    paddr = AddressTranslation(vaddr)
                    doubleword = MemoryLoad(paddr, DOUBLEWORD)
                    FR[fd] = doubleword
                 */
                LA64CpuState cpu = emulator.getCpuState();
                LA64FpuState fpu = emulator.getFpuState();
                MemoryLikeDevice memory = emulator.getMemory();
                LA64MemoryManagementUnit mmu = emulator.getMemoryManagementUnit();

                long rjValue = cpu.getGr(operands[1].value());
                long offset = operands[2].value();
                long vaddr = rjValue + offset;
                if ((vaddr & 0b111) != 0) {
                    throw new LA64RuntimeException(LA64Exception.ALE);
                }
                long paddr = mmu.translateVirtualAddress(vaddr, LA64MemoryManagementUnit.LA64MemoryAccessType.LOAD);
                fpu.setFrL(operands[0].value(), memory.loadDoubleWord(paddr));
                cpu.pcNext();
            }
        ));
        add(LA64InstructionInfo.formatFprGprSi12(
            "fst.d",
            0b0010_1011_11,
            (emulator, operands) -> {
                // fst.d fd, rj, si12
                /*
                    vaddr = GR[rj] + SignExtend(si12, GRLEN)
                    AddressComplianceCheck(vaddr)
                    paddr = AddressTranslation(vaddr)
                    MemoryStore(FR[fd][63:0], paddr, DOUBLEWORD)
                 */
                LA64CpuState cpu = emulator.getCpuState();
                LA64FpuState fpu = emulator.getFpuState();
                MemoryLikeDevice memory = emulator.getMemory();
                LA64MemoryManagementUnit mmu = emulator.getMemoryManagementUnit();

                long rjValue = cpu.getGr(operands[1].value());
                long offset = operands[2].value();
                long vaddr = rjValue + offset;
                if ((vaddr & 0b111) != 0) {
                    throw new LA64RuntimeException(LA64Exception.ALE);
                }
                long paddr = mmu.translateVirtualAddress(vaddr, LA64MemoryManagementUnit.LA64MemoryAccessType.STORE);
                memory.storeDoubleWord(paddr, fpu.getFrL(operands[0].value()));
                cpu.pcNext();
            }
        ));

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
        {
            LA64OperandType[] operandTypes = new LA64OperandType[]{LA64OperandType.CFR, LA64OperandType.OFFS21};
            BiFunction<LA64InstructionInfo, LA64Operand[], Integer> encoder = (info, ops) -> {
                // cj, offs21
                int cj = ops[0].value();
                int offs21 = ops[1].value();
                return info.opcode() | (offs21 & 0xFFFF) << 10 | (cj & 0b111) << 5 | (offs21 >> 16) & 0x1F;
            };
            Function<Integer, LA64Operand[]> decoder = (machineCode) -> {
                // cj, offs21
                int cj = BitMath.extractBits(machineCode, 5, 3);
                int offs21 = ((machineCode >> 10) & 0xFFFF) | ((machineCode & 0x1F) << 16);
                return new LA64Operand[]{LA64Operand.cfr(cj), LA64Operand.offs21(offs21)};
            };

            add(new LA64InstructionInfo(
                "bceqz",
                0b0100_10 << 26,
                0xFC000300,
                LA64InstructionFormat.MISCELLANEOUS,
                operandTypes,
                encoder,
                decoder,
                (emulator, operands) -> {
                    // bceqz cj, offs21
                    /*
                        if CFR[cj]==0 :
                            PC = PC + SignExtend({offs21, 2'b0}, GRLEN)
                     */
                    LA64CpuState cpu = emulator.getCpuState();
                    LA64FpuState fpu = emulator.getFpuState();
                    int cj = operands[0].value();
                    if (!fpu.getCfr(cj)) {
                        long offset = ((long) operands[1].value()) << 2;
                        long target = cpu.getPc() + offset;
                        cpu.setPc(target);
                    } else {
                        cpu.pcNext();
                    }
                }
            ));
            add(new LA64InstructionInfo(
                "bcnez",
                0b0100_10 << 26 | 0b01 << 8,
                0xFC000300,
                LA64InstructionFormat.MISCELLANEOUS,
                operandTypes,
                encoder,
                decoder,
                (emulator, operands) -> {
                    // bcnez cj, offs21
                    /*
                        if CFR[cj]!=0 :
                            PC = PC + SignExtend({offs21, 2'b0}, GRLEN)
                     */
                    LA64CpuState cpu = emulator.getCpuState();
                    LA64FpuState fpu = emulator.getFpuState();
                    int cj = operands[0].value();
                    if (fpu.getCfr(cj)) {
                        long offset = ((long) operands[1].value()) << 2;
                        long target = cpu.getPc() + offset;
                        cpu.setPc(target);
                    } else {
                        cpu.pcNext();
                    }
                }
            ));
        }
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
        add(LA64InstructionInfo.format2GPROffs16(
            "bltu",
            0b0110_10,
            (emulator, operands) -> {
                // bltu rj, rd, offs16
                /*
                    if unsigned(GR[rj]) < unsigned(GR[rd]) :
                        PC = PC + SignExtend({offs16, 2'b0}, GRLEN)
                 */
                BINARY_DOUBLE_WORD_BRANCH_EXECUTOR.execute(
                    emulator, operands,
                    (rj, rd) -> Long.compareUnsigned(rj, rd) < 0);
            }));
        add(LA64InstructionInfo.format2GPROffs16(
            "bgeu",
            0b0110_11,
            (emulator, operands) -> {
                // bgeu rj, rd, offs16
                /*
                    if unsigned(GR[rj]) >= unsigned(GR[rd]) :
                        PC = PC + SignExtend({offs16, 2'b0}, GRLEN)
                 */
                BINARY_DOUBLE_WORD_BRANCH_EXECUTOR.execute(
                    emulator, operands,
                    (rj, rd) -> Long.compareUnsigned(rj, rd) >= 0);
            }));
    }
}
