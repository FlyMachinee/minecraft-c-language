package net.flymachine.minecraftclanguage.content.logic.architecture.la64.isa.encoder;

import net.flymachine.minecraftclanguage.content.logic.architecture.la64.isa.instruction.LA64Instruction;
import net.flymachine.minecraftclanguage.content.logic.architecture.la64.isa.instruction.LA64InstructionInfo;
import net.flymachine.minecraftclanguage.content.logic.architecture.la64.isa.operand.LA64Operand;

import java.util.function.BiFunction;

public final class LA64Encoder {

    public static int encode(LA64Instruction instruction) {
        return encode(instruction.inst(), instruction.operands());
    }

    public static int encode(LA64InstructionInfo info, LA64Operand[] ops) {
        return switch (info.format()) {
            case FORMAT_3R -> encode3R(info.opcode(), ops);
            case FORMAT_2RI12 -> encode2RI12(info.opcode(), ops);
            case FORMAT_2RI16 -> encode2RI16(info.opcode(), ops);
            case MISCELLANEOUS -> {
                BiFunction<LA64InstructionInfo, LA64Operand[], Integer> encoder = info.encoder();
                if (encoder == null) {
                    throw new IllegalArgumentException(
                        "No encoder found for MISCELLANEOUS format instruction: " + info.format());
                }
                yield encoder.apply(info, ops);
            }
            default -> throw new IllegalArgumentException("Unsupported instruction format: " + info.format());
        };
    }

    private static int encode3R(int opcode, LA64Operand[] ops) {
        // rd, rj, rk
        int rd = ops[0].value();
        int rj = ops[1].value();
        int rk = ops[2].value();
        return (opcode << 15) | (rk << 10) | (rj << 5) | rd;
    }

    private static int encode2RI12(int opcode, LA64Operand[] ops) {
        // rd, rj, imm12
        int rd = ops[0].value();
        int rj = ops[1].value();
        int imm12 = ops[2].value();
        return (opcode << 22) | ((imm12 & 0xFFF) << 10) | (rj << 5) | rd;
    }

    private static int encode2RI16(int opcode, LA64Operand[] ops) {
        // rd, rj, imm16
        int rd = ops[0].value();
        int rj = ops[1].value();
        int imm16 = ops[2].value();
        return (opcode << 26) | ((imm16 & 0xFFFF) << 10) | (rj << 5) | rd;
    }
}
