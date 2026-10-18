package net.flymachine.minecraftclanguage.content.logic.emulator.la64;

import net.flymachine.minecraftclanguage.content.logic.architecture.la64.isa.instruction.LA64Instruction;
import net.flymachine.minecraftclanguage.content.logic.architecture.la64.isa.operand.LA64Operand;
import net.flymachine.minecraftclanguage.content.logic.architecture.la64.register.GeneralPurposeRegister;
import net.flymachine.minecraftclanguage.content.logic.profiler.branchPredictor.BranchInfo;
import net.flymachine.minecraftclanguage.content.logic.profiler.branchPredictor.BranchKind;
import org.jetbrains.annotations.Nullable;

/**
 * 把一条已解码的指令解析为分支预测所需的 {@link BranchInfo}
 */
final class BranchAnalyzer {

    private static final int RA = GeneralPurposeRegister.RA.getNumber();
    private static final int ZERO = GeneralPurposeRegister.ZERO.getNumber();

    private BranchAnalyzer() { }

    /**
     * @param inst 已解码的指令
     * @param pc   该指令的地址
     * @return 分支信息；若该指令不是分支则返回 {@code null}
     */
    static @Nullable BranchInfo analyze(LA64Instruction inst, long pc) {
        String mnemonic = inst.inst().mnemonic();
        LA64Operand[] operands = inst.operands();

        return switch (mnemonic) {
            case "b" -> new BranchInfo(pc, BranchKind.JUMP, pc + offsetAt(operands, 0));
            case "bl" -> new BranchInfo(pc, BranchKind.CALL, pc + offsetAt(operands, 0));
            case "jirl" -> jirl(pc, operands);
            case "beq", "bne", "blt", "bge", "bltu", "bgeu" ->
                new BranchInfo(pc, BranchKind.CONDITIONAL, pc + offsetAt(operands, 2));
            case "beqz", "bnez", "bceqz", "bcnez" ->
                new BranchInfo(pc, BranchKind.CONDITIONAL, pc + offsetAt(operands, 1));
            default -> null;
        };
    }

    private static long offsetAt(LA64Operand[] operands, int index) {
        return ((long) operands[index].value()) << 2;
    }

    private static BranchInfo jirl(long pc, LA64Operand[] operands) {
        int rd = operands[0].value();
        int rj = operands[1].value();

        BranchKind kind;
        if (rd == ZERO && rj == RA) {
            kind = BranchKind.RETURN;
        } else if (rd != ZERO) {
            kind = BranchKind.INDIRECT_CALL;
        } else {
            kind = BranchKind.INDIRECT_JUMP;
        }
        return new BranchInfo(pc, kind, 0L);
    }
}
