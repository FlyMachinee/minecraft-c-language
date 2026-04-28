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
 * @param opcode       指令操作码，不需要添加后缀零来凑 32 位
 * @param opcodeLength 操作码长度，单位为比特
 * @param format       指令格式，当值为 MISCELLANEOUS 时，指令格式由自定义编码器/解码器逻辑决定
 * @param operandTypes 指令操作数类型，按指令的汇编操作数顺序排列，而不是机器码中的顺序
 * @param encoder      自定义编码器，将指令信息和操作数转换为机器码
 * @param decoder      自定义解码器，将机器码转换为操作数
 * @param executor     自定义执行器，执行指令逻辑
 */
public record LA64InstructionInfo(
    @NotBlank String mnemonic,
    int opcode,
    int opcodeLength,
    LA64InstructionFormat format,
    LA64OperandType[] operandTypes,
    @Nullable BiFunction<LA64InstructionInfo, LA64Operand[], Integer> encoder,
    @Nullable Function<Integer, LA64Operand[]> decoder,
    @NotNull BiConsumer<LA64EmulatorHandler, LA64Operand[]> executor) { }
