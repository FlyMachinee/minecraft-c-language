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
import net.flymachine.minecraftclanguage.content.logic.assembler.la64.assembly.operand.LA64AsmSymOperand;
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

    private void collectSymbol(List<LA64AsmStatement> statements) {
        // 默认目标为 text 段
        Segment currentSegment = Segment.TEXT;

        // 每个段的当前偏移量
        EnumMap<Segment, Integer> offsets = new EnumMap<>(Segment.class);
        offsets.put(Segment.TEXT, 0);

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

                // 先判断是否是宏指令，如果不是再视为普通指令处理
                int offsetIncrease = switch (instruction.mnemonic()) {
                    case "li.w" -> getExpandLiWSize(ops);
                    case "ret", "sgt", "ble", "bgt", "move" -> 4;
                    case "sle", "sge", "seq", "sne" -> 8;
                    default -> {
                        if (!LA64InstructionSet.isMnemonic(instruction.mnemonic())) {
                            throw new IllegalArgumentException(
                                "Unsupported instruction mnemonic: " + instruction.mnemonic());
                        }
                        yield 4;
                    }
                };

                offsets.put(currentSegment, offset + offsetIncrease);
            } else {
                throw new UnsupportedOperationException("Unsupported statement: " + statement);
            }
        }
    }

    private LA64Object generateObject(List<LA64AsmStatement> statements) {

        // 默认目标为 text 段
        Segment currentSegment = Segment.TEXT;

        // 每个段的当前偏移量
        EnumMap<Segment, Integer> offsets = new EnumMap<>(Segment.class);
        offsets.put(Segment.TEXT, 0);

        // 每个段的内容
        EnumMap<Segment, ByteArrayOutputStream> segmentContents = new EnumMap<>(Segment.class);
        segmentContents.put(Segment.TEXT, new ByteArrayOutputStream());

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

            // 标签已经处理
            if (statement instanceof LA64AsmLabel) {
                continue;
            }

            // 指令处理
            if (statement instanceof LA64AsmInstruction instruction) {

                List<LA64AsmOperand> ops = instruction.operands();

                // TODO: 检查是否为 text 段
                ByteArrayOutputStream out = segmentContents.get(currentSegment);

                // 先判断是否是宏指令，如果不是再视为普通指令处理
                int offsetIncrease = switch (instruction.mnemonic()) {
                    case "li.w" -> expandLiW(ops, out);
                    case "ret" -> expandRet(out);
                    case "move" -> expandMove(ops, out);
                    case "bgt" -> expandBgt(currentSegment, ops, offset, out);
                    case "ble" -> expandBle(currentSegment, ops, offset, out);
                    case "sle" -> expandSle(ops, out);
                    case "sgt" -> expandSgt(ops, out);
                    case "sge" -> expandSge(ops, out);
                    case "seq" -> expandSeq(ops, out);
                    case "sne" -> expandSne(ops, out);
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
                                if (asmOp instanceof LA64Register reg) {
                                    value = reg.getNumber();
                                } else if (asmOp instanceof LA64AsmImmOperand imm) {
                                    value = (int) imm.value();
                                } else if (asmOp instanceof LA64AsmSymOperand sym) {
                                    SymbolLocation loc = symbolTable.get(sym.symbol());
                                    if (loc == null) {
                                        throw new IllegalArgumentException("Undefined symbol: " + sym.symbol());
                                    }
                                    if (loc.segment != currentSegment) {
                                        throw new IllegalArgumentException(
                                            "Current cannot support cross-segment symbol references");
                                    }
                                    value = loc.offset - offset;
                                    if ((value & 0b11) != 0) {
                                        throw new IllegalArgumentException("Offset not aligned: " + value);
                                    }
                                    value >>= 2;
                                    if (!type.representable(value)) {
                                        throw new IllegalArgumentException(
                                            "Symbol offset out of range for operand type " + type + ": " + value);
                                    }
                                } else {
                                    throw new IllegalArgumentException(
                                        "Unsupported operand type: " + asmOp.getClass().getName());
                                }
                                return new LA64Operand(type, value);
                            })
                            .toArray(LA64Operand[]::new);
                        writeIntLittleEndian(out, LA64Encoder.encode(info, convertedOps));
                        yield 4;
                    }
                };

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

    private int expandLiW(List<LA64AsmOperand> ops, ByteArrayOutputStream out) {
        // TODO: 检查操作数类型
        // li.w dst, imm32
        LA64Register dst = ((LA64Register) ops.get(0));
        int value = (int) ((LA64AsmImmOperand) ops.get(1)).value();

        if (BitMath.isSi12(value)) {
            // addi.w rd, zero, imm12
            writeFormat2RSi12(out, "addi.w", dst, GeneralPurposeRegister.ZERO, value);
            return 4;
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
            writeFormat2RUi12(out, "ori", dst, dst, lower12);
            return 8;
        }
    }

    private int getExpandLiWSize(List<LA64AsmOperand> ops) {
        // TODO: 检查操作数类型
        // li.w dst, imm32
        int value = (int) ((LA64AsmImmOperand) ops.get(1)).value();
        if (BitMath.isSi12(value)) {
            // addi.w rd, zero, imm12
            return 4;
        } else {
            // lu12i.w rd, upper20
            // ori rd, rd, lower12
            return 8;
        }
    }

    private int expandRet(ByteArrayOutputStream out) {
        // jirl zero, ra, 0
        // 可尝试替换为硬编码的数据，但可读性差
        writeFormat2ROffs16(out, "jirl", GeneralPurposeRegister.ZERO, GeneralPurposeRegister.RA, 0);
        return 4;
    }

    private int expandMove(List<LA64AsmOperand> ops, ByteArrayOutputStream out) {
        // move rd, rj
        // TODO: 强转前检测
        LA64Register dst = ((LA64Register) ops.get(0));
        LA64Register src = ((LA64Register) ops.get(1));

        // or rd, rj, zero
        writeFormat3R(out, "or", dst, src, GeneralPurposeRegister.ZERO);
        return 4;
    }

    private int expandBgt(Segment currentSegment, List<LA64AsmOperand> ops, int offset, ByteArrayOutputStream out) {
        // bgt rj, rd, offs16
        LA64Register rj = ((LA64Register) ops.get(0));
        LA64Register rd = ((LA64Register) ops.get(1));
        // => blt rd, rj, offs16
        if (ops.get(2) instanceof LA64AsmSymOperand sym) {
            int offs16 = getPcOffs16Offset(sym, currentSegment, offset);
            writeFormat2ROffs16(out, "blt", rd, rj, offs16);
            return 4;
        } else {
            throw new IllegalArgumentException("Expected sym, but got " + ops.get(2));
        }
    }

    private int expandBle(Segment currentSegment, List<LA64AsmOperand> ops, int offset, ByteArrayOutputStream out) {
        // ble rj, rd, offs16
        LA64Register rj = ((LA64Register) ops.get(0));
        LA64Register rd = ((LA64Register) ops.get(1));
        // => bge rd, rj, offs16
        if (ops.get(2) instanceof LA64AsmSymOperand sym) {
            int offs16 = getPcOffs16Offset(sym, currentSegment, offset);
            writeFormat2ROffs16(out, "bge", rd, rj, offs16);
            return 4;
        } else {
            throw new IllegalArgumentException("Expected sym, but got " + ops.get(2));
        }
    }

    private int expandSle(List<LA64AsmOperand> ops, ByteArrayOutputStream out) {
        // sle rd, rj, rk
        // (a <= b) => !(b < a)
        // slt rd, rk, rj
        // xori rd, rd, 1
        LA64Register rd = ((LA64Register) ops.get(0));
        LA64Register rj = ((LA64Register) ops.get(1));
        LA64Register rk = ((LA64Register) ops.get(2));

        writeFormat3R(out, "slt", rd, rk, rj);
        writeFormat2RUi12(out, "xori", rd, rd, 1);
        return 8;
    }

    private int expandSgt(List<LA64AsmOperand> ops, ByteArrayOutputStream out) {
        // sgt rd, rj, rk
        // (a > b) => (b < a)
        // slt rd, rk, rj
        LA64Register rd = ((LA64Register) ops.get(0));
        LA64Register rj = ((LA64Register) ops.get(1));
        LA64Register rk = ((LA64Register) ops.get(2));

        writeFormat3R(out, "slt", rd, rk, rj);
        return 4;
    }

    private int expandSge(List<LA64AsmOperand> ops, ByteArrayOutputStream out) {
        // sge rd, rj, rk
        // (a >= b) => !(a < b)
        // slt rd, rj, rk
        // xori rd, rd, 1
        LA64Register rd = ((LA64Register) ops.get(0));
        LA64Register rj = ((LA64Register) ops.get(1));
        LA64Register rk = ((LA64Register) ops.get(2));

        writeFormat3R(out, "slt", rd, rj, rk);
        writeFormat2RUi12(out, "xori", rd, rd, 1);
        return 8;
    }

    private int expandSeq(List<LA64AsmOperand> ops, ByteArrayOutputStream out) {
        // seq rd, rj, rk
        // =>
        // xor rd, rj, rk
        // sltui rd, rd, 1
        LA64Register rd = ((LA64Register) ops.get(0));
        LA64Register rj = ((LA64Register) ops.get(1));
        LA64Register rk = ((LA64Register) ops.get(2));

        writeFormat3R(out, "xor", rd, rj, rk);
        writeFormat2RSi12(out, "sltui", rd, rd, 1);
        return 8;
    }

    private int expandSne(List<LA64AsmOperand> ops, ByteArrayOutputStream out) {
        // sne rd, rj, rk
        // =>
        // xor rd, rj, rk
        // sltu rd, zero, rd
        LA64Register rd = ((LA64Register) ops.get(0));
        LA64Register rj = ((LA64Register) ops.get(1));
        LA64Register rk = ((LA64Register) ops.get(2));

        writeFormat3R(out, "xor", rd, rj, rk);
        writeFormat3R(out, "sltu", rd, GeneralPurposeRegister.ZERO, rd);
        return 8;
    }

    private int getPcOffs16Offset(LA64AsmSymOperand symbol, Segment currentSegment, int currentOffset) {
        SymbolLocation loc = symbolTable.get(symbol.symbol());
        if (loc == null) {
            throw new IllegalArgumentException("Undefined symbol: " + symbol.symbol());
        }
        if (loc.segment != currentSegment) {
            throw new IllegalArgumentException("Current cannot support cross-segment symbol references");
        }
        int offset = loc.offset - currentOffset;
        if ((offset & 0b11) != 0) {
            throw new IllegalArgumentException("Offset not aligned: " + offset);
        }
        offset >>= 2;
        if (!BitMath.isOffs16(offset)) {
            throw new IllegalArgumentException("Offset out of range for offs16: " + offset);
        }
        return offset;
    }

    private static void writeFormat3R(
        ByteArrayOutputStream out, String mnemonic, LA64Register rd, LA64Register rj, LA64Register rk) {

        Optional<LA64InstructionInfo> optionalInfo = LA64InstructionSet.getByMnemonic(mnemonic);
        if (optionalInfo.isEmpty()) {
            throw new IllegalArgumentException("Unsupported instruction mnemonic: " + mnemonic);
        }
        LA64InstructionInfo info = optionalInfo.get();
        writeIntLittleEndian(
            out, LA64Encoder.encode(
                info, new LA64Operand[]{
                    LA64Operand.reg(rd),
                    LA64Operand.reg(rj),
                    LA64Operand.reg(rk)}));
    }

    private static void writeFormat2RSi12(
        ByteArrayOutputStream out, String mnemonic, LA64Register rd, LA64Register rj, int si12) {

        if (!BitMath.isSi12(si12)) {
            throw new IllegalArgumentException("Immediate value out of range for si12: " + si12);
        }
        Optional<LA64InstructionInfo> optionalInfo = LA64InstructionSet.getByMnemonic(mnemonic);
        if (optionalInfo.isEmpty()) {
            throw new IllegalArgumentException("Unsupported instruction mnemonic: " + mnemonic);
        }
        LA64InstructionInfo info = optionalInfo.get();
        writeIntLittleEndian(
            out, LA64Encoder.encode(
                info, new LA64Operand[]{
                    LA64Operand.reg(rd),
                    LA64Operand.reg(rj),
                    LA64Operand.si12(si12)}));
    }

    private static void writeFormat2RUi12(
        ByteArrayOutputStream out, String mnemonic, LA64Register rd, LA64Register rj, int ui12) {

        if (!BitMath.isUi12(ui12)) {
            throw new IllegalArgumentException("Immediate value out of range for ui12: " + ui12);
        }
        Optional<LA64InstructionInfo> optionalInfo = LA64InstructionSet.getByMnemonic(mnemonic);
        if (optionalInfo.isEmpty()) {
            throw new IllegalArgumentException("Unsupported instruction mnemonic: " + mnemonic);
        }
        LA64InstructionInfo info = optionalInfo.get();
        writeIntLittleEndian(
            out, LA64Encoder.encode(
                info, new LA64Operand[]{
                    LA64Operand.reg(rd),
                    LA64Operand.reg(rj),
                    LA64Operand.ui12(ui12)}));
    }

    private static void writeFormat2ROffs16(
        ByteArrayOutputStream out, String mnemonic, LA64Register rd, LA64Register rj, int offs16) {

        if (!BitMath.isOffs16(offs16)) {
            throw new IllegalArgumentException("Immediate value out of range for offs16: " + offs16);
        }
        Optional<LA64InstructionInfo> optionalInfo = LA64InstructionSet.getByMnemonic(mnemonic);
        if (optionalInfo.isEmpty()) {
            throw new IllegalArgumentException("Unsupported instruction mnemonic: " + mnemonic);
        }
        LA64InstructionInfo info = optionalInfo.get();
        writeIntLittleEndian(
            out, LA64Encoder.encode(
                info, new LA64Operand[]{
                    LA64Operand.reg(rd),
                    LA64Operand.reg(rj),
                    LA64Operand.offs16(offs16)}));
    }

    private static void writeIntLittleEndian(ByteArrayOutputStream out, int value) {
        out.write(value & 0xFF);
        out.write((value >> 8) & 0xFF);
        out.write((value >> 16) & 0xFF);
        out.write((value >> 24) & 0xFF);
    }
}
