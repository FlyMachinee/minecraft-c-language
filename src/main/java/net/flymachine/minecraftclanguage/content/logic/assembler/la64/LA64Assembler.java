package net.flymachine.minecraftclanguage.content.logic.assembler.la64;

import net.flymachine.minecraftclanguage.content.logic.architecture.la64.isa.encoder.LA64Encoder;
import net.flymachine.minecraftclanguage.content.logic.architecture.la64.isa.instruction.LA64InstructionInfo;
import net.flymachine.minecraftclanguage.content.logic.architecture.la64.isa.instruction.LA64InstructionSet;
import net.flymachine.minecraftclanguage.content.logic.architecture.la64.isa.operand.LA64Operand;
import net.flymachine.minecraftclanguage.content.logic.architecture.la64.isa.operand.LA64OperandType;
import net.flymachine.minecraftclanguage.content.logic.architecture.la64.register.GeneralPurposeRegister;
import net.flymachine.minecraftclanguage.content.logic.architecture.la64.register.LA64Register;
import net.flymachine.minecraftclanguage.content.logic.architecture.la64.register.LA64RegisterResolver;
import net.flymachine.minecraftclanguage.content.logic.architecture.la64.util.BitMath;
import net.flymachine.minecraftclanguage.content.logic.assembler.la64.assembly.LA64AsmDirective;
import net.flymachine.minecraftclanguage.content.logic.assembler.la64.assembly.LA64AsmInstruction;
import net.flymachine.minecraftclanguage.content.logic.assembler.la64.assembly.LA64AsmLabel;
import net.flymachine.minecraftclanguage.content.logic.assembler.la64.assembly.LA64AsmStatement;
import net.flymachine.minecraftclanguage.content.logic.assembler.la64.assembly.operand.LA64AsmImmOperand;
import net.flymachine.minecraftclanguage.content.logic.assembler.la64.assembly.operand.LA64AsmOperand;
import net.flymachine.minecraftclanguage.content.logic.memory.Segment;
import net.flymachine.minecraftclanguage.content.logic.object.la64.LA64Object;

import java.io.ByteArrayOutputStream;
import java.util.*;
import java.util.stream.IntStream;

public final class LA64Assembler {

    public LA64Assembler() { }

    public LA64Object assemble(List<LA64AsmStatement> statements) {
        symbolTable.clear();
        collectSymbol(statements);
        return generateObject(statements);
    }

    private static class SymbolLocation {
        public final Segment segment;
        public int offset;

        SymbolLocation(Segment segment, int offset) {
            this.segment = segment;
            this.offset = offset;
        }
    }

    private final Map<String, SymbolLocation> symbolTable = new HashMap<>();

    private void collectSymbol(List<LA64AsmStatement> statements) { }

    private LA64Object generateObject(List<LA64AsmStatement> statements) {

        // 默认目标为 text 段
        Segment currentSegment = Segment.TEXT;

        // 每个段的当前偏移量
        EnumMap<Segment, Integer> offsets = new EnumMap<>(Segment.class);
        offsets.put(Segment.TEXT, 0);

        // 每个段的内容
        EnumMap<Segment, ByteArrayOutputStream> segmentContents = new EnumMap<>(Segment.class);
        segmentContents.put(Segment.TEXT, new ByteArrayOutputStream());

        // 用于查询寄存器名
        LA64RegisterResolver resolver = LA64RegisterResolver.getInstance();

        for (LA64AsmStatement statement : statements) {

            // 伪指令处理
            if (statement instanceof LA64AsmDirective directive) {
                switch (directive.name()) {
                    case "global" -> { }
                    default -> throw new UnsupportedOperationException("Unsupported directive: " + directive.name());
                }
                continue;
            }

            // 当前段偏移
            int offset = offsets.get(currentSegment);

            // 标签处理
            if (statement instanceof LA64AsmLabel label) {
                String labelName = label.name();

                // 不能是寄存器名
                if (resolver.isRegisterName(labelName)) {
                    throw new IllegalArgumentException("Label cannot be a register name: " + labelName);
                }

                // 不能重复
                if (symbolTable.containsKey(labelName)) {
                    throw new IllegalArgumentException("Label already exists: " + labelName);
                }

                // 添加至符号表
                symbolTable.put(labelName, new SymbolLocation(currentSegment, offset));
                continue;
            }

            // 指令处理
            if (statement instanceof LA64AsmInstruction instruction) {

                List<LA64AsmOperand> ops = instruction.operands();

                // 段偏移量的增加量
                int offsetIncrease = 4;

                // TODO: 检查是否为 text 段
                ByteArrayOutputStream out = segmentContents.get(currentSegment);

                // 先判断是否是宏指令，如果不是再视为普通指令处理
                switch (instruction.mnemonic()) {
                    case "li.w" -> {
                        // TODO: 检查操作数类型
                        // li.w dst, imm32
                        LA64Register dst = ((LA64Register) ops.get(0));
                        int value = (int) ((LA64AsmImmOperand) ops.get(1)).value();

                        if (BitMath.isSi12(value)) {
                            // addi.w rd, zero, imm12
                            writeIntLittleEndian(
                                out, LA64Encoder.encode(
                                    LA64InstructionSet.getByMnemonic("addi.w").orElseThrow(), new LA64Operand[]{
                                        LA64Operand.reg(dst), // rd
                                        LA64Operand.reg(GeneralPurposeRegister.ZERO), // zero
                                        LA64Operand.si12(value) // imm12
                                    }));
                        } else {
                            // lu12i.w rd, upper20
                            // ori rd, rd, lower12
                            int lower12 = value & 0xFFF;
                            int upper20 = value >>> 12;
                            writeIntLittleEndian(
                                out, LA64Encoder.encode(
                                    LA64InstructionSet.getByMnemonic("lu12i.w").orElseThrow(), new LA64Operand[]{
                                        LA64Operand.reg(dst), // rd
                                        LA64Operand.si20(upper20) // upper20
                                    }));
                            writeIntLittleEndian(
                                out, LA64Encoder.encode(
                                    LA64InstructionSet.getByMnemonic("ori").orElseThrow(), new LA64Operand[]{
                                        LA64Operand.reg(dst), // rd
                                        LA64Operand.reg(dst), // rd
                                        LA64Operand.ui12(lower12) // lower12
                                    }));
                            offsetIncrease = 8;
                        }
                    }
                    case "ret" -> // jirl zero, ra, 0
                        // 可尝试替换为硬编码的数据，但可读性差
                        writeIntLittleEndian(
                            out, LA64Encoder.encode(
                                LA64InstructionSet.getByMnemonic("jirl").orElseThrow(), new LA64Operand[]{
                                    LA64Operand.reg(GeneralPurposeRegister.ZERO), // zero
                                    LA64Operand.reg(GeneralPurposeRegister.RA), // ra
                                    LA64Operand.offs16(0) // 0
                                }));
                    case "move" -> {
                        // move rd, rj
                        // TODO: 强转前检测
                        LA64Register dst = ((LA64Register) ops.get(0));
                        LA64Register src = ((LA64Register) ops.get(1));

                        // or rd, rj, zero
                        writeIntLittleEndian(
                            out, LA64Encoder.encode(
                                LA64InstructionSet.getByMnemonic("or").orElseThrow(), new LA64Operand[]{
                                    LA64Operand.reg(dst),
                                    LA64Operand.reg(src),
                                    LA64Operand.reg(GeneralPurposeRegister.ZERO)}));
                    }
                    default -> {
                        Optional<LA64InstructionInfo>
                            optionalInfo = LA64InstructionSet.getByMnemonic(instruction.mnemonic());

                        if (optionalInfo.isEmpty()) {
                            throw new IllegalArgumentException(
                                "Unsupported instruction mnemonic: " + instruction.mnemonic());
                        }

                        LA64InstructionInfo info = optionalInfo.get();
                        LA64Operand[] convertedOps = IntStream
                            .range(0, ops.size())
                            .mapToObj(i -> {
                                LA64AsmOperand asmOp = ops.get(i);
                                LA64OperandType type = info.operandTypes()[i];
                                checkOpType(type, asmOp);

                                int value;
                                if (asmOp instanceof LA64Register regOp) {
                                    value = regOp.getNumber();
                                } else if (asmOp instanceof LA64AsmImmOperand immOp) {
                                    value = (int) immOp.value();
                                } else {
                                    throw new IllegalArgumentException(
                                        "Unsupported operand type: " + asmOp.getClass().getName());
                                }
                                return new LA64Operand(type, value);
                            })
                            .toArray(LA64Operand[]::new);
                        writeIntLittleEndian(out, LA64Encoder.encode(info, convertedOps));
                    }
                }

                offsets.put(currentSegment, offset + offsetIncrease);
            } else {
                throw new UnsupportedOperationException("Unsupported statement: " + statement);
            }
        }

        return new LA64Object(segmentContents.get(Segment.TEXT).toByteArray());
    }

    private static void checkOpType(LA64OperandType type, LA64AsmOperand asmOp) {
        switch (type) {
            case GPR, FPR -> {
                if (!(asmOp instanceof LA64Register)) {
                    throw new IllegalArgumentException(
                        "Expected register operand for type " + type + ", but got: " +
                        asmOp.getClass().getName());
                }
            }
            case SI12, SI20, UI12, OFFS16 -> {
                if (asmOp instanceof LA64Register) {
                    throw new IllegalArgumentException(
                        "Expected immediate operand for type " + type + ", but got register: " +
                        ((LA64Register) asmOp).getPrimaryName());
                }
            }
        }
    }

    private static void writeIntLittleEndian(ByteArrayOutputStream out, int value) {
        out.write(value & 0xFF);
        out.write((value >> 8) & 0xFF);
        out.write((value >> 16) & 0xFF);
        out.write((value >> 24) & 0xFF);
    }
}
