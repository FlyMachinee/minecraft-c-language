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
import net.flymachine.minecraftclanguage.content.logic.assembler.la64.assembly.*;
import net.flymachine.minecraftclanguage.content.logic.assembler.la64.assembly.operand.LA64AsmImmOperand;
import net.flymachine.minecraftclanguage.content.logic.assembler.la64.assembly.operand.LA64AsmOperand;
import net.flymachine.minecraftclanguage.content.logic.assembler.la64.assembly.operand.LA64AsmSymOperand;
import net.flymachine.minecraftclanguage.content.logic.object.*;
import net.flymachine.minecraftclanguage.content.logic.object.la64.LA64Object;

import java.io.ByteArrayOutputStream;
import java.util.*;

public final class LA64Assembler {

    public LA64Assembler() { }

    public LA64Object assemble(LA64Assembly assembly) {
        symbolTable.clear();
        globalSymbols.clear();
        if (!checkArgAndOperand(assembly)) {
            return null;
        }
        collectSymbol(assembly);
        return generateObject(assembly);
    }

    private static class SymbolLocation {
        public final SectionType sectionType;
        public int offset;

        SymbolLocation(SectionType sectionType, int offset) {
            this.sectionType = sectionType;
            this.offset = offset;
        }
    }

    private final Map<String, SymbolLocation> symbolTable = new HashMap<>();
    private final Set<String> globalSymbols = new HashSet<>();

    // 用于查询寄存器名
    private final LA64RegisterResolver resolver = LA64RegisterResolver.getInstance();

    private boolean checkArgAndOperand(LA64Assembly assembly) {
        for (LA64AsmStatement statement : assembly.stmts()) {
            // 伪指令检查
            if (statement instanceof LA64AsmDirective directive) {
                switch (directive.name()) {
                    case "globl", "global" -> {
                        if (directive.argCount() != 1 && !directive.arg(0).isSym()) {
                            throw new IllegalArgumentException(
                                "Expected symbol argument for global directive, but got: " + directive.args().get(0));
                        } else {
                            String symbolName = directive.arg(0).asSym();
                            if (symbolName.startsWith(".L")) {
                                throw new IllegalArgumentException(
                                    "Local labels cannot be declared global: " + symbolName);
                            }
                        }
                    }
                    case "text", "data", "bss" -> {
                        if (directive.argCount() != 0) {
                            throw new IllegalArgumentException(
                                "Expected no argument for text, but got: " + directive.args());
                        }
                    }
                    case "align", "balign" -> {
                        if (directive.argCount() != 1 && !directive.arg(0).isNum()) {
                            throw new IllegalArgumentException(
                                "Expected numeric argument for align/balign directive, but got: " +
                                directive.args().get(0));
                        }
                    }
                    case "long", "word" -> {
                        if (directive.argCount() != 1 && !directive.arg(0).isNum()) {
                            throw new IllegalArgumentException(
                                "Expected numeric argument for word/long directive, but got: " +
                                directive.args().get(0));
                        } else {
                            long data = directive.arg(0).asNum();
                            if (Integer.MIN_VALUE > data || data > Integer.MAX_VALUE) {
                                // 非 32 位数值
                                throw new IllegalArgumentException("Word value out of range: " + data);
                            }
                        }
                    }
                    case "quad", "dword" -> {
                        if (directive.argCount() != 1 && !directive.arg(0).isNum()) {
                            throw new IllegalArgumentException(
                                "Expected numeric argument for word/long directive, but got: " +
                                directive.args().get(0));
                        }
                    }
                    case "zero" -> {
                        if (directive.argCount() != 1 && !directive.arg(0).isNum()) {
                            throw new IllegalArgumentException(
                                "Expected numeric argument for zero directive, but got: " + directive.args().get(0));
                        } else {
                            long count = directive.arg(0).asNum();
                            if (count < 0 || count > Integer.MAX_VALUE) {
                                throw new IllegalArgumentException("Zero size out of range: " + count);
                            }
                        }
                    }
                    default -> throw new UnsupportedOperationException("Unsupported directive: " + directive.name());
                }
                continue;
            }

            // 标签处理
            if (statement instanceof LA64AsmLabel) {
                continue;
            }

            // 指令处理
            if (statement instanceof LA64AsmInstruction instruction) {
                List<LA64AsmOperand> ops = instruction.operands();

                // 先判断是否是宏指令，如果不是再视为普通指令处理
                switch (instruction.mnemonic()) {
                    case "li.w" -> {
                        // li.w rd, imm32
                        if (ops.size() != 2 || !ops.get(0).isGpr() || !ops.get(1).isImm()) {
                            throw new IllegalArgumentException(
                                "Expected operands: reg, imm32 for li.w, but got: " + ops);
                        }
                        long imm32 = ops.get(1).asImm();
                        if (imm32 < Integer.MIN_VALUE || imm32 > Integer.MAX_VALUE) {
                            throw new IllegalArgumentException("Immediate value out of range for li.w: " + imm32);
                        }
                    }
                    case "li.d" -> {
                        // li.d rd, imm64
                        if (ops.size() != 2 || !ops.get(0).isGpr() || !ops.get(1).isImm()) {
                            throw new IllegalArgumentException(
                                "Expected operands: reg, imm64 for li.d, but got: " + ops);
                        }
                    }
                    case "ret" -> {
                        // ret
                        if (!ops.isEmpty()) {
                            throw new IllegalArgumentException(
                                "Expected no operand for ret, but got: " + ops);
                        }
                    }
                    case "sle", "sge", "sleu", "sgeu", "seq", "sne" -> {
                        // xxx rd, rj, rk
                        if (ops.size() != 3 || !ops.get(0).isGpr() || !ops.get(1).isGpr() || !ops.get(2).isGpr()) {
                            throw new IllegalArgumentException(
                                "Expected operands: reg, reg, reg for " + instruction.mnemonic() + ", but got: " + ops);
                        }
                    }
                    case "move" -> {
                        // move rd, rj
                        if (ops.size() != 2 || !ops.get(0).isGpr() || !ops.get(1).isGpr()) {
                            throw new IllegalArgumentException(
                                "Expected operands: reg, reg for move, but got: " + ops);
                        }
                    }
                    case "la.abs" -> {
                        // la.abs rd, sym
                        if (ops.size() != 2 || !ops.get(0).isGpr() || !ops.get(1).isSym()) {
                            throw new IllegalArgumentException(
                                "Expected operands: reg, sym for la.abs, but got: " + ops);
                        }
                    }
                    case "la.local", "la.pcrel" -> {
                        // la.pcrel rd, sym
                        // la.pcrel rd, rj, sym
                        if (!(ops.size() == 2 && ops.get(0).isGpr() && ops.get(1).isSym()) &&
                            !(ops.size() == 3 && ops.get(0).isGpr() && ops.get(1).isGpr() && ops.get(2).isSym())) {
                            throw new IllegalArgumentException(
                                "Expected operands: reg, sym or reg, reg, sym for " + instruction.mnemonic() +
                                ", but got: " + ops);
                        }
                    }
                    default -> {
                        Optional<LA64InstructionInfo>
                            optionalInfo = LA64InstructionSet.getByMnemonic(instruction.mnemonic());

                        if (optionalInfo.isEmpty()) {
                            throw new IllegalArgumentException(
                                "Unsupported instruction mnemonic: " + instruction.mnemonic());
                        }

                        LA64InstructionInfo info = optionalInfo.get();
                        if (info.operandTypes().length != ops.size()) {
                            throw new IllegalArgumentException(
                                "Expected " + info.operandTypes().length + " operands for instruction " +
                                instruction.mnemonic() + ", but got: " + ops.size());
                        }
                        for (int i = 0; i < ops.size(); i++) {
                            LA64OperandType expected = info.operandTypes()[i];
                            LA64AsmOperand current = ops.get(i);
                            boolean matched = switch (expected) {
                                case GPR -> current.isGpr();
                                case FPR -> current.isFpr();
                                case CFR -> current.isCfr();
                                case UI5, UI6, UI12, SI12, SI20 -> {
                                    if (!current.isImm()) {
                                        yield false;
                                    }
                                    long imm = current.asImm();
                                    if (imm < Integer.MIN_VALUE || imm > Integer.MAX_VALUE) {
                                        yield false;
                                    }
                                    yield switch (expected) {
                                        case UI5 -> BitMath.isUi5((int) imm);
                                        case UI6 -> BitMath.isUi6((int) imm);
                                        case UI12 -> BitMath.isUi12((int) imm);
                                        case SI12 -> BitMath.isSi12((int) imm);
                                        case SI20 -> BitMath.isSi20((int) imm);
                                        default -> throw new IllegalStateException("Unexpected value: " + expected);
                                    };
                                }
                                case OFFS16, OFFS21, OFFS26 -> {
                                    if (current.isSym()) {
                                        yield true; // 暂时认为在范围内
                                    }
                                    if (!current.isImm()) {
                                        yield false;
                                    }
                                    long imm = current.asImm();
                                    if (imm < Integer.MIN_VALUE || imm > Integer.MAX_VALUE) {
                                        yield false;
                                    }
                                    int imm32 = (int) imm;
                                    if ((imm32 & 0b11) != 0) {
                                        // 地址必须 4 字节对齐
                                        yield false;
                                    }
                                    imm32 >>= 2;
                                    yield switch (expected) {
                                        case OFFS16 -> BitMath.isOffs16(imm32);
                                        case OFFS21 -> BitMath.isOffs21(imm32);
                                        case OFFS26 -> BitMath.isOffs26(imm32);
                                        default -> throw new IllegalStateException("Unexpected value: " + expected);
                                    };
                                }
                            };
                            if (!matched) {
                                throw new IllegalArgumentException(
                                    "Operand type mismatch for operand " + (i + 1) + " of instruction " +
                                    instruction.mnemonic() + ": expected " + expected + ", but got: " + current);
                            }
                        }
                    }
                }
                continue;
            }

            throw new UnsupportedOperationException("Unsupported statement: " + statement);
        }
        return true;
    }

    private void collectSymbol(LA64Assembly assembly) {
        // 默认目标为 text 节
        SectionType currentSectionType = SectionType.TEXT;

        // 每个节的当前偏移量
        EnumMap<SectionType, Integer> offsets = new EnumMap<>(SectionType.class);
        offsets.put(SectionType.TEXT, 0);
        offsets.put(SectionType.DATA, 0);
        offsets.put(SectionType.BSS, 0);

        for (LA64AsmStatement statement : assembly.stmts()) {

            // 当前节偏移
            int offset = offsets.get(currentSectionType);

            // 伪指令处理
            if (statement instanceof LA64AsmDirective directive) {
                switch (directive.name()) {
                    case "globl", "global" -> {
                        String symbolName = directive.arg(0).asSym();
                        if (symbolName.startsWith(".L")) {
                            throw new IllegalArgumentException(
                                "Local labels cannot be declared global: " + symbolName);
                        }
                        globalSymbols.add(symbolName);
                    }
                    case "text", "data", "bss" ->
                        currentSectionType = SectionType.valueOf(directive.name().toUpperCase());
                    case "align", "balign" -> {
                        long alignment = directive.arg(0).asNum();
                        offsets.put(currentSectionType, BitMath.alignUp(offset, (int) alignment));
                    }
                    case "long", "word" -> {
                        if (currentSectionType == SectionType.BSS) {
                            throw new IllegalArgumentException(".word directive cannot be used in .bss section");
                        }
                        offsets.put(currentSectionType, offset + 4);
                    }
                    case "quad", "dword" -> {
                        if (currentSectionType == SectionType.BSS) {
                            throw new IllegalArgumentException(".word directive cannot be used in .bss section");
                        }
                        offsets.put(currentSectionType, offset + 8);
                    }
                    case "zero" -> {
                        long count = directive.arg(0).asNum();
                        offsets.put(currentSectionType, offset + (int) count);
                    }
                    default -> throw new UnsupportedOperationException("Unsupported directive: " + directive.name());
                }
                continue;
            }

            // 标签处理
            if (statement instanceof LA64AsmLabel label) {
                String labelName = label.name();
                // 不能重复
                if (symbolTable.containsKey(labelName)) {
                    throw new IllegalArgumentException("Label already exists: " + labelName);
                }
                // 添加至符号表
                symbolTable.put(labelName, new SymbolLocation(currentSectionType, offset));
                continue;
            }

            // 指令处理
            if (statement instanceof LA64AsmInstruction instruction) {
                if (currentSectionType != SectionType.TEXT) {
                    throw new IllegalArgumentException(
                        "Only .text section can contain instructions, but current section is: ." +
                        currentSectionType.toString().toLowerCase());
                }

                List<LA64AsmOperand> ops = instruction.operands();

                // 先判断是否是宏指令，如果不是再视为普通指令处理
                int offsetIncrease = switch (instruction.mnemonic()) {
                    case "li.w" -> getExpandLiWSize(ops);
                    case "li.d" -> getExpandLiDSize(ops);
                    case "sle", "sge", "sleu", "sgeu", "seq", "sne" -> 8;
                    case "la.abs" -> 16;
                    case "la.local", "la.pcrel" -> {
                        if (ops.size() == 2) {
                            // la.pcrel $rd, sym
                            yield 2 * 4;
                        } else {
                            // la.pcrel $rd, $rj, sym
                            yield 5 * 4;
                        }
                    }
                    default -> 4;
                };

                offsets.put(currentSectionType, offset + offsetIncrease);
            } else {
                throw new UnsupportedOperationException("Unsupported statement: " + statement);
            }
        }
    }

    private List<String> symbolNames;

    private int getSymbolNameIndexOrAdd(String symbolName) {
        int index = symbolNames.indexOf(symbolName);
        if (index == -1) {
            index = symbolNames.size();
            symbolNames.add(symbolName);
        }
        return index;
    }

    private LA64Object generateObject(LA64Assembly assembly) {

        // 默认目标为 text 节
        SectionType currentSectionType = SectionType.TEXT;

        // 每个节的内容
        EnumMap<SectionType, ByteArrayOutputStream> sectionContents = new EnumMap<>(SectionType.class);
        EnumMap<SectionType, List<RelocationEntry>> sectionRelocations = new EnumMap<>(SectionType.class);
        EnumMap<SectionType, Integer> sectionMaxAlign = new EnumMap<>(SectionType.class);
        for (SectionType type : SectionType.values()) {
            sectionMaxAlign.put(type, 1);
            if (type == SectionType.BSS) {
                continue;
            }
            sectionContents.put(type, new ByteArrayOutputStream());
            sectionRelocations.put(type, new ArrayList<>());
        }
        int bssSize = 0;

        // 符号表等
        List<SymbolEntry> symbolList = new ArrayList<>();
        symbolNames = new ArrayList<>();
        for (Map.Entry<String, SymbolLocation> entry : symbolTable.entrySet()) {
            String symbolName = entry.getKey();
            if (symbolName.startsWith(".L")) {
                continue;
            }

            SymbolLocation loc = entry.getValue();
            int symbolNameIndex = symbolNames.size();
            symbolNames.add(symbolName);
            symbolList.add(new SymbolEntry(
                symbolNameIndex,
                loc.sectionType,
                loc.offset,
                globalSymbols.contains(symbolName)));
        }

        for (LA64AsmStatement statement : assembly.stmts()) {

            // 当前节内容与重定位表
            ByteArrayOutputStream out = sectionContents.get(currentSectionType);
            List<RelocationEntry> relocs = sectionRelocations.get(currentSectionType);

            // 当前节偏移
            int offset = currentSectionType == SectionType.BSS ? bssSize : out.size();

            // 伪指令处理
            if (statement instanceof LA64AsmDirective directive) {
                switch (directive.name()) {
                    case "globl", "global" -> { }
                    case "text", "data", "bss" ->
                        currentSectionType = SectionType.valueOf(directive.name().toUpperCase());
                    case "align", "balign" -> {
                        int align = (int) directive.arg(0).asNum();
                        int newOffset = BitMath.alignUp(offset, align);
                        if (newOffset > offset) {
                            if (currentSectionType != SectionType.BSS) {
                                // 使用 0 进行填充
                                for (int i = 0; i < newOffset - offset; i++) {
                                    out.write(0);
                                }
                            } else {
                                bssSize = newOffset;
                            }
                        }
                        // 更新当前节的最大对齐要求
                        sectionMaxAlign.computeIfPresent(
                            currentSectionType,
                            (k, currentMaxAlign) -> Math.max(currentMaxAlign, align));
                    }
                    case "long", "word" -> writeIntLittleEndian(out, (int) directive.arg(0).asNum());
                    case "quad", "dword" -> writeLongLittleEndian(out, directive.arg(0).asNum());
                    case "zero" -> {
                        int count = (int) directive.arg(0).asNum();
                        if (currentSectionType != SectionType.BSS) {
                            for (int i = 0; i < count; i++) {
                                out.write(0);
                            }
                        } else {
                            bssSize += count;
                        }
                    }
                    default -> throw new UnsupportedOperationException("Unsupported directive: " + directive.name());
                }
                continue;
            }

            // 标签已经处理
            if (statement instanceof LA64AsmLabel) {
                continue;
            }

            // 指令处理
            if (statement instanceof LA64AsmInstruction instruction) {

                List<LA64AsmOperand> ops = instruction.operands();

                // 先判断是否是宏指令，如果不是再视为普通指令处理
                switch (instruction.mnemonic()) {
                    case "li.w" -> expandLiW(ops, out);
                    case "li.d" -> expandLiD(ops, out);
                    case "ret" -> expandRet(out);
                    case "move" -> expandMove(ops, out);
                    case "sle" -> expandSle(ops, out);
                    case "sge" -> expandSge(ops, out);
                    case "sleu" -> expandSleu(ops, out);
                    case "sgeu" -> expandSgeu(ops, out);
                    case "seq" -> expandSeq(ops, out);
                    case "sne" -> expandSne(ops, out);
                    case "la.abs" -> expandLaAbs(ops, out, offset, relocs);
                    case "la.local", "la.pcrel" -> {
                        if (ops.size() == 2) {
                            expandLaPcRel(ops, out, offset, relocs);
                        } else {
                            throw new IllegalArgumentException("Not supported yet");
                        }
                    }
                    default -> {
                        LA64InstructionInfo info =
                            LA64InstructionSet.getByMnemonic(instruction.mnemonic()).orElseThrow();
                        List<LA64Operand> convertedOps = new ArrayList<>();
                        for (int i = 0; i < ops.size(); i++) {
                            LA64OperandType type = info.operandTypes()[i];
                            LA64AsmOperand asmOp = ops.get(i);

                            int value = 0;
                            if (asmOp instanceof LA64Register reg) {
                                value = reg.getNumber();
                            } else if (asmOp instanceof LA64AsmImmOperand imm) {
                                value = switch (type) {
                                    case OFFS16, OFFS21, OFFS26 -> ((int) imm.value()) >> 2;
                                    default -> (int) imm.value();
                                };
                            } else if (asmOp instanceof LA64AsmSymOperand sym) {
                                value = getPcRelOffset(sym, currentSectionType, offset, info, type, relocs);
                            }
                            convertedOps.add(new LA64Operand(type, value));
                        }
                        writeIntLittleEndian(out, LA64Encoder.encode(info, convertedOps.toArray(new LA64Operand[0])));
                    }
                }
            } else {
                throw new UnsupportedOperationException("Unsupported statement: " + statement);
            }
        }

        String newName = assembly.fileName().replaceAll("\\.[^.]+$", "") + ".o";
        List<Section> sections = new ArrayList<>();
        for (SectionType type : SectionType.values()) {
            int maxAlign = sectionMaxAlign.get(type);
            if (type == SectionType.BSS) {
                // .bss 节特殊处理
                if (bssSize != 0) {
                    // 节不为空则添加指节头表
                    sections.add(new Section(type, bssSize, maxAlign));
                }
            } else {
                // 默认节处理
                byte[] data = sectionContents.get(type).toByteArray();
                if (data.length != 0) {
                    // 节不为空则添加指节头表
                    List<RelocationEntry> relocList = sectionRelocations.get(type);
                    sections.add(new Section(type, data, maxAlign, relocList));
                }
            }
        }
        return new LA64Object(newName, sections, symbolList, symbolNames);
    }

    private static RelocationType getRelocationType(String mnemonic) {
        return switch (mnemonic) {
            case "beqz", "bnez" -> RelocationType.R_LARCH_B21;
            case "b", "bl" -> RelocationType.R_LARCH_B26;
            case "jirl", "beq", "bne", "blt", "bge" -> RelocationType.R_LARCH_B16;
            default ->
                throw new IllegalArgumentException("Unsupported instruction mnemonic for relocation: " + mnemonic);
        };
    }

    private void expandLiW(List<LA64AsmOperand> ops, ByteArrayOutputStream out) {
        // li.w dst, imm32
        LA64Register dst = ops.get(0).asGpr();
        int value = (int) ops.get(1).asImm();

        if (BitMath.isSi12(value)) {
            // addi.w rd, zero, imm12
            writeFormat2RSi12(out, "addi.w", dst, GeneralPurposeRegister.ZERO, value);
        } else {
            // lu12i.w rd, upper20 si20
            // ori rd, rd, lower12 ui12
            int lower12 = BitMath.extractBits(value, 12);
            int upper20 = BitMath.extractSignedBits(value, 12, 20);
            writeFormat1RSi20(out, "lu12i.w", dst, upper20);
            writeFormat2RUi12(out, "ori", dst, dst, lower12);
        }
    }

    private void expandLiD(List<LA64AsmOperand> ops, ByteArrayOutputStream out) {
        // li.w dst, imm64
        LA64Register dst = ops.get(0).asGpr();
        long value = ops.get(1).asImm();

        if (value >= Integer.MIN_VALUE && value <= Integer.MAX_VALUE) {
            // imm64 -> imm32
            expandLiW(ops, out);
        } else {
            // lu12i.w   rd, hi20       si20
            // ori       rd, rd, low12  ui12
            // lu32i.d   rd, h_low20    si20
            // lu52i.d   rd, rd, h_hi12 si12
            int hi20 = (int) BitMath.extractSignedBits(value, 12, 20);
            int low12 = (int) BitMath.extractBits(value, 12);
            int hLow20 = (int) BitMath.extractSignedBits(value, 32, 20);

            writeFormat1RSi20(out, "lu12i.w", dst, hi20);
            writeFormat2RUi12(out, "ori", dst, dst, low12);
            writeFormat1RSi20(out, "lu32i.d", dst, hLow20);

            int hHi12 = (int) BitMath.extractSignedBits(value, 52, 12);
            if (hHi12 != 0 && hHi12 != -1) {
                writeFormat2RSi12(out, "lu52i.d", dst, dst, hHi12);
            }
        }
    }

    private int getExpandLiWSize(List<LA64AsmOperand> ops) {
        // li.w dst, imm32
        int value = (int) ops.get(1).asImm();
        if (BitMath.isSi12(value)) {
            // addi.w rd, zero, imm12
            return 4;
        } else {
            // lu12i.w rd, upper20
            // ori rd, rd, lower12
            return 8;
        }
    }

    private int getExpandLiDSize(List<LA64AsmOperand> ops) {
        // li.d dst, imm64
        long value = ops.get(1).asImm();

        if (value >= Integer.MIN_VALUE && value <= Integer.MAX_VALUE) {
            // imm64 -> imm32
            return getExpandLiWSize(ops);
        } else {
            int hHi12 = (int) BitMath.extractSignedBits(value, 52, 12);
            if (hHi12 != 0 && hHi12 != -1) {
                return 16;
            }
            return 12;
        }
    }

    private void expandRet(ByteArrayOutputStream out) {
        // jirl zero, ra, 0
        // 可尝试替换为硬编码的数据，但可读性差
        writeFormat2ROffs16(out, "jirl", GeneralPurposeRegister.ZERO, GeneralPurposeRegister.RA, 0);
    }

    private void expandMove(List<LA64AsmOperand> ops, ByteArrayOutputStream out) {
        // move rd, rj
        LA64Register dst = ops.get(0).asGpr();
        LA64Register src = ops.get(1).asGpr();

        // or rd, rj, zero
        writeFormat3R(out, "or", dst, src, GeneralPurposeRegister.ZERO);
    }

    private void expandSle(List<LA64AsmOperand> ops, ByteArrayOutputStream out) {
        // sle rd, rj, rk
        // (a <= b) => !(b < a)
        // slt rd, rk, rj
        // xori rd, rd, 1
        LA64Register rd = ops.get(0).asGpr();
        LA64Register rj = ops.get(1).asGpr();
        LA64Register rk = ops.get(2).asGpr();

        writeFormat3R(out, "slt", rd, rk, rj);
        writeFormat2RUi12(out, "xori", rd, rd, 1);
    }

    private void expandSge(List<LA64AsmOperand> ops, ByteArrayOutputStream out) {
        // sge rd, rj, rk
        // (a >= b) => !(a < b)
        // slt rd, rj, rk
        // xori rd, rd, 1
        LA64Register rd = ops.get(0).asGpr();
        LA64Register rj = ops.get(1).asGpr();
        LA64Register rk = ops.get(2).asGpr();

        writeFormat3R(out, "slt", rd, rj, rk);
        writeFormat2RUi12(out, "xori", rd, rd, 1);
    }

    private void expandSleu(List<LA64AsmOperand> ops, ByteArrayOutputStream out) {
        // sleu rd, rj, rk
        // (a <= b) => !(b < a)
        // sltu rd, rk, rj
        // xori rd, rd, 1
        LA64Register rd = ops.get(0).asGpr();
        LA64Register rj = ops.get(1).asGpr();
        LA64Register rk = ops.get(2).asGpr();

        writeFormat3R(out, "sltu", rd, rk, rj);
        writeFormat2RUi12(out, "xori", rd, rd, 1);
    }

    private void expandSgeu(List<LA64AsmOperand> ops, ByteArrayOutputStream out) {
        // sgeu rd, rj, rk
        // (a >= b) => !(a < b)
        // sltu rd, rj, rk
        // xori rd, rd, 1
        LA64Register rd = ops.get(0).asGpr();
        LA64Register rj = ops.get(1).asGpr();
        LA64Register rk = ops.get(2).asGpr();

        writeFormat3R(out, "sltu", rd, rj, rk);
        writeFormat2RUi12(out, "xori", rd, rd, 1);
    }

    private void expandSeq(List<LA64AsmOperand> ops, ByteArrayOutputStream out) {
        // seq rd, rj, rk
        // =>
        // xor rd, rj, rk
        // sltui rd, rd, 1
        LA64Register rd = ops.get(0).asGpr();
        LA64Register rj = ops.get(1).asGpr();
        LA64Register rk = ops.get(2).asGpr();

        writeFormat3R(out, "xor", rd, rj, rk);
        writeFormat2RSi12(out, "sltui", rd, rd, 1);
    }

    private void expandSne(List<LA64AsmOperand> ops, ByteArrayOutputStream out) {
        // sne rd, rj, rk
        // =>
        // xor rd, rj, rk
        // sltu rd, zero, rd
        LA64Register rd = ops.get(0).asGpr();
        LA64Register rj = ops.get(1).asGpr();
        LA64Register rk = ops.get(2).asGpr();

        writeFormat3R(out, "xor", rd, rj, rk);
        writeFormat3R(out, "sltu", rd, GeneralPurposeRegister.ZERO, rd);
    }

    private void expandLaAbs(
        List<LA64AsmOperand> ops, ByteArrayOutputStream out, int offset, List<RelocationEntry> relocList) {
        // la.abs rd, sym
        // =>
        // lu12i.w   rd, %abs_hi20(sym)       # R_LARCH_ABS_HI20        si20
        // ori       rd, rd, %abs_lo12(sym)   # R_LARCH_ABS_LO12        ui12
        // lu32i.d   rd, %abs64_lo20(sym)     # R_LARCH_ABS64_LO20      si20
        // lu52i.d   rd, rd, %abs64_hi12(sym) # R_LARCH_ABS64_HI12      si12

        LA64Register rd = ops.get(0).asGpr();
        String sym = ops.get(1).asSym();

        int symbolNameIndex = getSymbolNameIndexOrAdd(sym);

        writeFormat1RSi20(out, "lu12i.w", rd, 0);
        writeFormat2RUi12(out, "ori", rd, rd, 0);
        writeFormat1RSi20(out, "lu32i.d", rd, 0);
        writeFormat2RSi12(out, "lu52i.d", rd, rd, 0);

        relocList.add(new RelocationEntry(offset, symbolNameIndex, RelocationType.R_LARCH_ABS_HI20));
        relocList.add(new RelocationEntry(offset + 4, symbolNameIndex, RelocationType.R_LARCH_ABS_LO12));
        relocList.add(new RelocationEntry(offset + 8, symbolNameIndex, RelocationType.R_LARCH_ABS64_LO20));
        relocList.add(new RelocationEntry(offset + 12, symbolNameIndex, RelocationType.R_LARCH_ABS64_HI12));
    }

    private void expandLaPcRel(
        List<LA64AsmOperand> ops, ByteArrayOutputStream out, int offset, List<RelocationEntry> relocList) {
        // la.local/la.pcrel rd, sym
        // =>
        // pcalau12i  $rd, %pc_hi20(sym)        # R_LARCH_PCALA_HI20       si20
        // addi.d     $rd, $rd, %pc_lo12(sym)   # R_LARCH_PCALA_LO12       si12

        LA64Register rd = ops.get(0).asGpr();
        String sym = ops.get(1).asSym();

        int symbolNameIndex = getSymbolNameIndexOrAdd(sym);

        writeFormat1RSi20(out, "pcalau12i", rd, 0);
        writeFormat2RSi12(out, "addi.d", rd, rd, 0);

        relocList.add(new RelocationEntry(offset, symbolNameIndex, RelocationType.R_LARCH_PCALA_HI20));
        relocList.add(new RelocationEntry(offset + 4, symbolNameIndex, RelocationType.R_LARCH_PCALA_LO12));
    }

    private int getPcRelOffset(
        LA64AsmSymOperand symbol, SectionType currentSectionType, int currentOffset, LA64InstructionInfo info,
        LA64OperandType type, List<RelocationEntry> relocList) {

        SymbolLocation loc = symbolTable.get(symbol.name());

        if (loc == null) {
            // 未定义符号
            if (symbol.name().startsWith(".L")) {
                throw new IllegalArgumentException("Undefined local symbol: " + symbol.name());
            }

            // 非局部符号，添加至重定位表
            int symbolNameIndex = getSymbolNameIndexOrAdd(symbol.name());
            relocList.add(new RelocationEntry(currentOffset, symbolNameIndex, getRelocationType(info.mnemonic())));
            return 0;
        } else {
            if (loc.sectionType != currentSectionType) {
                throw new IllegalArgumentException(
                    "Cannot jump to sectionType other than .text: " + symbol.name() + " in sectionType " +
                    loc.sectionType);
            }
            int value = loc.offset - currentOffset;
            if ((value & 0b11) != 0) {
                throw new IllegalArgumentException("Offset not aligned: " + value);
            }
            value >>= 2;
            if (!type.representable(value)) {
                throw new IllegalArgumentException(
                    "Symbol offset out of range for operand type " + type + ": " + value);
            }
            return value;
        }
    }

    private static void writeFormat3R(
        ByteArrayOutputStream out, String mnemonic, LA64Register rd, LA64Register rj, LA64Register rk) {

        Optional<LA64InstructionInfo> optionalInfo = LA64InstructionSet.getByMnemonic(mnemonic);
        if (optionalInfo.isEmpty()) {
            throw new IllegalArgumentException("Unsupported instruction mnemonic: " + mnemonic);
        }
        LA64InstructionInfo info = optionalInfo.get();
        writeIntLittleEndian(
            out, LA64Encoder.encode(info, LA64Operand.reg(rd), LA64Operand.reg(rj), LA64Operand.reg(rk)));
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
            out, LA64Encoder.encode(info, LA64Operand.reg(rd), LA64Operand.reg(rj), LA64Operand.si12(si12)));
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
            out, LA64Encoder.encode(info, LA64Operand.reg(rd), LA64Operand.reg(rj), LA64Operand.ui12(ui12)));
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
            out, LA64Encoder.encode(info, LA64Operand.reg(rd), LA64Operand.reg(rj), LA64Operand.offs16(offs16)));
    }

    private static void writeFormat1RSi20(ByteArrayOutputStream out, String mnemonic, LA64Register rd, int si20) {
        if (!BitMath.isSi20(si20)) {
            throw new IllegalArgumentException("Immediate value out of range for si20: " + si20);
        }
        Optional<LA64InstructionInfo> optionalInfo = LA64InstructionSet.getByMnemonic(mnemonic);
        if (optionalInfo.isEmpty()) {
            throw new IllegalArgumentException("Unsupported instruction mnemonic: " + mnemonic);
        }
        LA64InstructionInfo info = optionalInfo.get();
        writeIntLittleEndian(out, LA64Encoder.encode(info, LA64Operand.reg(rd), LA64Operand.si20(si20)));
    }

    private static void writeIntLittleEndian(ByteArrayOutputStream out, int value) {
        out.write(value & 0xFF);
        out.write((value >> 8) & 0xFF);
        out.write((value >> 16) & 0xFF);
        out.write((value >> 24) & 0xFF);
    }

    private static void writeLongLittleEndian(ByteArrayOutputStream out, long value) {
        out.write((int) (value & 0xFF));
        out.write((int) ((value >> 8) & 0xFF));
        out.write((int) ((value >> 16) & 0xFF));
        out.write((int) ((value >> 24) & 0xFF));
        out.write((int) ((value >> 32) & 0xFF));
        out.write((int) ((value >> 40) & 0xFF));
        out.write((int) ((value >> 48) & 0xFF));
        out.write((int) ((value >> 56) & 0xFF));
    }
}
