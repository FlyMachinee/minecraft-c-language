package net.flymachine.minecraftclanguage.content.logic.assembler.la64;

import net.flymachine.minecraftclanguage.content.logic.architecture.la64.isa.encoder.LA64Encoder;
import net.flymachine.minecraftclanguage.content.logic.architecture.la64.isa.instruction.LA64InstructionSet;
import net.flymachine.minecraftclanguage.content.logic.architecture.la64.isa.operand.LA64Operand;
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
import net.flymachine.minecraftclanguage.content.logic.assembler.la64.assembly.operand.LA64AsmRegOperand;
import net.flymachine.minecraftclanguage.content.logic.memory.Segment;
import net.flymachine.minecraftclanguage.content.logic.object.la64.LA64Object;

import java.io.ByteArrayOutputStream;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
                        // TODO: 检查操作数类型？或者也可不需要
                        LA64Register dst = ((LA64AsmRegOperand) ops.get(0)).reg();
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
                            // 暂时不支持 32 位立即数
                            throw new IllegalArgumentException(
                                "Instruction operands must be a Si12 value: " + instruction.mnemonic());
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
                    default -> {
                        // TODO: 暂时不支持其他
                        throw new IllegalArgumentException(
                            "Unsupported instruction mnemonic: " + instruction.mnemonic());
                    }
                }

                offsets.put(currentSegment, offset + offsetIncrease);
            } else {
                throw new UnsupportedOperationException("Unsupported statement: " + statement);
            }
        }

        return new LA64Object(segmentContents.get(Segment.TEXT).toByteArray());
    }

    private void writeIntLittleEndian(ByteArrayOutputStream out, int value) {
        out.write(value & 0xFF);
        out.write((value >> 8) & 0xFF);
        out.write((value >> 16) & 0xFF);
        out.write((value >> 24) & 0xFF);
    }
}
