package net.flymachine.minecraftclanguage.content.logic.architecture.la64.isa.instruction;

import net.flymachine.minecraftclanguage.content.logic.architecture.la64.isa.operand.LA64Operand;
import net.flymachine.minecraftclanguage.content.logic.architecture.la64.isa.operand.LA64OperandType;
import net.flymachine.minecraftclanguage.content.logic.emulator.la64.LA64EmulatorHandler;
import org.apache.logging.log4j.core.config.plugins.validation.constraints.NotBlank;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Function;

/**
 * @param mnemonic     指令助记符
 * @param opcode       指令操作码
 * @param mask         操作码掩码，指示操作码的位置
 * @param format       指令格式，当值为 MISCELLANEOUS 时，指令格式由自定义编码器/解码器逻辑决定
 * @param operandTypes 指令操作数类型，按指令的汇编操作数顺序排列，而不是机器码中的顺序
 * @param encoder      自定义编码器，将指令信息和操作数转换为机器码
 * @param decoder      自定义解码器，将机器码转换为操作数
 * @param executor     自定义执行器，执行指令逻辑
 */
public record LA64InstructionInfo(
    @NotBlank String mnemonic,
    int opcode,
    int mask,
    LA64InstructionFormat format,
    LA64OperandType[] operandTypes,
    @Nullable BiFunction<LA64InstructionInfo, LA64Operand[], Integer> encoder,
    @Nullable Function<Integer, LA64Operand[]> decoder,
    @NotNull BiConsumer<LA64EmulatorHandler, LA64Operand[]> executor) {

    public LA64InstructionInfo(
        @NotBlank String mnemonic,
        int opcode,
        int mask,
        LA64InstructionFormat format,
        LA64OperandType[] operandTypes,
        @NotNull BiConsumer<LA64EmulatorHandler, LA64Operand[]> executor) {
        this(mnemonic, opcode, mask, format, operandTypes, null, null, executor);
    }

    public static LA64InstructionInfo format2Fpr(
        @NotBlank String mnemonic,
        int opcode,
        @NotNull BiConsumer<LA64EmulatorHandler, LA64Operand[]> executor) {
        return new LA64InstructionInfo(
            mnemonic,
            opcode << 10,
            ((1 << 22) - 1) << 10,
            LA64InstructionFormat.FORMAT_2R,
            LA64OperandType.FORMAT_2FPR_OPTYPE,
            executor
        );
    }

    public static LA64InstructionInfo format3Gpr(
        @NotBlank String mnemonic,
        int opcode,
        @NotNull BiConsumer<LA64EmulatorHandler, LA64Operand[]> executor) {
        return new LA64InstructionInfo(
            mnemonic,
            opcode << 15,
            ((1 << 17) - 1) << 15,
            LA64InstructionFormat.FORMAT_3R,
            LA64OperandType.FORMAT_3GPR_OPTYPE,
            executor
        );
    }

    public static LA64InstructionInfo format3Fpr(
        @NotBlank String mnemonic,
        int opcode,
        @NotNull BiConsumer<LA64EmulatorHandler, LA64Operand[]> executor) {
        return new LA64InstructionInfo(
            mnemonic,
            opcode << 15,
            ((1 << 17) - 1) << 15,
            LA64InstructionFormat.FORMAT_3R,
            LA64OperandType.FORMAT_3FPR_OPTYPE,
            executor
        );
    }

    public static LA64InstructionInfo format2GprSi12(
        @NotBlank String mnemonic,
        int opcode,
        @NotNull BiConsumer<LA64EmulatorHandler, LA64Operand[]> executor) {
        return new LA64InstructionInfo(
            mnemonic,
            opcode << 22,
            ((1 << 10) - 1) << 22,
            LA64InstructionFormat.FORMAT_2RI12,
            LA64OperandType.FORMAT_2GPR_SI12_OPTYPE,
            executor
        );
    }

    public static LA64InstructionInfo formatFprGprSi12(
        @NotBlank String mnemonic,
        int opcode,
        @NotNull BiConsumer<LA64EmulatorHandler, LA64Operand[]> executor) {
        return new LA64InstructionInfo(
            mnemonic,
            opcode << 22,
            ((1 << 10) - 1) << 22,
            LA64InstructionFormat.FORMAT_2RI12,
            LA64OperandType.FORMAT_FPR_GPR_SI12_OPTYPE,
            executor
        );
    }

    public static LA64InstructionInfo format2GprUi12(
        @NotBlank String mnemonic,
        int opcode,
        @NotNull BiConsumer<LA64EmulatorHandler, LA64Operand[]> executor) {
        return new LA64InstructionInfo(
            mnemonic,
            opcode << 22,
            ((1 << 10) - 1) << 22,
            LA64InstructionFormat.FORMAT_2RI12,
            LA64OperandType.FORMAT_2GPR_UI12_OPTYPE,
            executor
        );
    }

    public static LA64InstructionInfo format2GPROffs16(
        @NotBlank String mnemonic,
        int opcode,
        @NotNull BiConsumer<LA64EmulatorHandler, LA64Operand[]> executor) {
        return new LA64InstructionInfo(
            mnemonic,
            opcode << 26,
            ((1 << 6) - 1) << 26,
            LA64InstructionFormat.FORMAT_2RI16,
            LA64OperandType.FORMAT_2GPR_OFFS16_OPTYPE,
            executor
        );
    }

    public static LA64InstructionInfo format1GPROffs21(
        @NotBlank String mnemonic,
        int opcode,
        @NotNull BiConsumer<LA64EmulatorHandler, LA64Operand[]> executor) {
        return new LA64InstructionInfo(
            mnemonic,
            opcode << 26,
            ((1 << 6) - 1) << 26,
            LA64InstructionFormat.FORMAT_1RI21,
            LA64OperandType.FORMAT_1GPR_OFFS21_OPTYPE,
            executor
        );
    }

    public static LA64InstructionInfo formatOffs26(
        @NotBlank String mnemonic,
        int opcode,
        @NotNull BiConsumer<LA64EmulatorHandler, LA64Operand[]> executor) {
        return new LA64InstructionInfo(
            mnemonic,
            opcode << 26,
            ((1 << 6) - 1) << 26,
            LA64InstructionFormat.FORMAT_I26,
            LA64OperandType.FORMAT_OFFS26_OPTYPE,
            executor
        );
    }

}
