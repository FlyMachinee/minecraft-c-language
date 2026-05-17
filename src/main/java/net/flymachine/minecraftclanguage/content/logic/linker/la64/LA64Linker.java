package net.flymachine.minecraftclanguage.content.logic.linker.la64;

import it.unimi.dsi.fastutil.ints.Int2ObjectArrayMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import net.flymachine.minecraftclanguage.content.logic.architecture.la64.util.BitMath;
import net.flymachine.minecraftclanguage.content.logic.executable.Segment;
import net.flymachine.minecraftclanguage.content.logic.executable.SegmentPermission;
import net.flymachine.minecraftclanguage.content.logic.executable.la64.LA64Executable;
import net.flymachine.minecraftclanguage.content.logic.linker.LinkOptions;
import net.flymachine.minecraftclanguage.content.logic.object.*;
import net.flymachine.minecraftclanguage.content.logic.object.la64.LA64Object;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.*;

public final class LA64Linker {

    public LA64Linker() { }

    private LinkOptions options = LinkOptions.DEFAULT;

    public void setOptions(LinkOptions options) {
        this.options = options;
    }

    private static class GlobalSymbol {
        long virtualAddress;
        int objectIndex;
        int symIndexInObject; // 该符号在所在文件的符号名表中的 index
        boolean isAbsolute; // 表示该符号的地址是否为绝对地址

        public GlobalSymbol(long virtualAddress, int objectIndex, int symIndexInObject, boolean isAbsolute) {
            this.virtualAddress = virtualAddress;
            this.objectIndex = objectIndex;
            this.symIndexInObject = symIndexInObject;
            this.isAbsolute = isAbsolute;
        }
    }

    private static class LocalSymbol {
        long virtualAddress;
        int symIndexInObject;
        boolean isAbsolute;

        public LocalSymbol(long virtualAddress, int symIndexInObject, boolean isAbsolute) {
            this.virtualAddress = virtualAddress;
            this.symIndexInObject = symIndexInObject;
            this.isAbsolute = isAbsolute;
        }
    }

    // 记录节在输出段中的位置
    private static class PlacementInfo {
        final int segmentIndex;
        final int offsetInSegment;

        PlacementInfo(int segIdx, int off) {
            this.segmentIndex = segIdx;
            this.offsetInSegment = off;
        }
    }

    private static class OutputSegment {
        final int index;
        final long wantedAddr;
        final Set<SegmentPermission> perms;
        long finalAddr;
        private ByteArrayOutputStream dataStream = new ByteArrayOutputStream();
        private byte[] data;
        int totalSize = 0;
        int align = 1;
        // final List<SectionContribution> contributions = new ArrayList<>();

        OutputSegment(int idx, long wanted, Set<SegmentPermission> perms) {
            this.index = idx;
            this.wantedAddr = wanted;
            this.perms = perms;
        }

        int addDataSection(byte[] data, int align) {
            int offset = dataStream.size();
            // 简单对齐
            if (align > 1) {
                int pad = (align - (offset % align)) % align;
                for (int i = 0; i < pad; i++) {
                    dataStream.write(0);
                }
                offset = dataStream.size();
                this.align = Math.max(align, this.align);
            }
            try {
                dataStream.write(data);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            // contributions.add(new SectionContribution(offset, data.length, false));
            totalSize = dataStream.size();
            return offset;
        }

        int addBssSection(int size, int align) {
            int offset = totalSize;
            if (align > 1) {
                int pad = (align - (offset % align)) % align;
                offset += pad;
                totalSize += pad;
                this.align = Math.max(align, this.align);
            }
            // contributions.add(new SectionContribution(offset, size, true));
            totalSize += size;
            return offset;
        }

        byte[] getData() {
            if (data == null) {
                data = dataStream.toByteArray();
                dataStream = null;
            }
            return data;
        }

        // record SectionContribution(int offsetInSegment, int size, boolean isBss) { }
    }

    public LA64Executable link(LA64Object... objects) {
        if (objects.length == 0) {
            throw new IllegalArgumentException("Cannot link an empty object");
        }

        // 处理链接选项
        // 段信息
        Int2ObjectMap<OutputSegment> outputSegments = new Int2ObjectArrayMap<>();
        for (LinkOptions.SegmentConfig config : options.segmentConfigs()) {
            if (outputSegments.containsKey(config.segmentIndex())) {
                throw new IllegalArgumentException(
                    "Duplicate segment config for segment index: " + config.segmentIndex());
            }
            outputSegments.put(
                config.segmentIndex(),
                new OutputSegment(config.segmentIndex(), config.virtualAddr(), config.permissions()));
        }

        // 记录每个目标文件中每个节在输出段中的偏移
        Int2ObjectMap<EnumMap<SectionType, PlacementInfo>> placements = new Int2ObjectArrayMap<>();
        for (int i = 0; i < objects.length; i++) {
            placements.put(i, new EnumMap<>(SectionType.class));
        }
        // 收集数据至输出段，记录每个节的偏移
        // 按照各个节在链接选项中出现的先后顺序进行合并
        EnumSet<SectionType> seenSections = EnumSet.noneOf(SectionType.class);
        int seenBssInSegmentIndex = -1;
        for (LinkOptions.SegmentMapping mapping : options.segmentMappings()) {
            // 当前遍历节
            SectionType secType = mapping.sectionType();
            if (!seenSections.add(secType)) {
                throw new IllegalArgumentException(
                    "Section ." + secType.toString().toLowerCase() + " is mapped multiple times");
            }

            // 当前遍历节需要合并到的段索引
            int segmentIndex = mapping.segmentIndex();
            if (!outputSegments.containsKey(segmentIndex)) {
                throw new IllegalArgumentException("Missing segment config for segment index: " + segmentIndex);
            }
            if (secType == SectionType.BSS) {
                seenBssInSegmentIndex = segmentIndex;
            } else {
                if (seenBssInSegmentIndex != -1 && segmentIndex == seenBssInSegmentIndex) {
                    throw new IllegalArgumentException(
                        "Section ." + secType.toString().toLowerCase() +
                        " cannot be mapped after .bss in a single segment, i.e. .bss must be at last of the segment");
                }
            }

            // 遍历所有输入文件，将指定节的数据追加到对应输出段末尾，并记录位置
            OutputSegment outSeg = outputSegments.get(segmentIndex);
            for (int i = 0; i < objects.length; i++) {
                LA64Object obj = objects[i];
                Section sec = obj.sections().stream().filter(s -> s.type() == secType).findFirst().orElse(null);
                if (sec == null) {
                    continue;
                }
                int offsetInSeg;
                if (secType == SectionType.BSS) {
                    offsetInSeg = outSeg.addBssSection(sec.size(), sec.align());
                } else {
                    offsetInSeg = outSeg.addDataSection(sec.data(), sec.align());
                }
                // 第 i 个输入文件的 secType 节被放置在 segmentIndex 段的 offsetInSeg 处
                placements.get(i).put(secType, new PlacementInfo(segmentIndex, offsetInSeg));
            }
        }

        // 为每个输出段分配虚拟地址
        List<OutputSegment> sortedOutputSegments = new ArrayList<>(outputSegments.values());
        sortedOutputSegments.sort(Comparator.comparingInt(seg -> seg.index));
        for (int i = 0; i < outputSegments.size(); i++) {
            OutputSegment seg = sortedOutputSegments.get(i);
            long currStart = seg.wantedAddr;
            if (currStart != 0) {
                // 如果链接选项中指定了该段的虚拟地址，则使用该地址
                // 检查是否满足对齐要求
                if (currStart != BitMath.alignUp(currStart, seg.align)) {
                    throw new IllegalArgumentException(
                        "Specified virtual address for segment " + seg.index + " does not satisfy alignment " +
                        "requirement of " + seg.align + ": " + currStart);
                }
                // 检查是否与先前段的地址范围重叠
                long currEnd = currStart + seg.totalSize;
                for (int j = 0; j < i; ++j) {
                    OutputSegment prevSeg = sortedOutputSegments.get(j);
                    long prevStart = prevSeg.finalAddr;
                    long prevEnd = prevStart + prevSeg.totalSize;
                    if (currStart < prevEnd && currEnd > prevStart) {
                        throw new IllegalArgumentException(
                            "Specified virtual address for segment " + seg.index +
                            " overlaps with segment " + prevSeg.index + ": [" + currStart + ", " + currEnd +
                            ") vs [" + prevStart + ", " + prevEnd + ")");
                    }
                }
                // 没有问题
                seg.finalAddr = currStart;
            } else {
                // 否则自动分配地址，分配至前一段末尾的下一个页开始处
                if (i != 0) {
                    OutputSegment prevSeg = sortedOutputSegments.get(i - 1);
                    currStart = prevSeg.finalAddr + prevSeg.totalSize;
                }
                seg.finalAddr = BitMath.alignUp(currStart, Math.max(4096, seg.align));
            }
        }

        // 构建全局符号表与各文件的局部符号表
        Map<String, GlobalSymbol> globalSymbols = new HashMap<>();
        Map<Integer, Map<String, LocalSymbol>> localSymbols = new HashMap<>();
        for (int i = 0; i < objects.length; i++) {
            LA64Object obj = objects[i];
            EnumMap<SectionType, PlacementInfo> placementInfo = placements.get(i);

            List<SymbolEntry> symbols = obj.symbols();
            List<String> names = obj.symbolNames();

            Map<String, LocalSymbol> localSymbolMap = new HashMap<>();

            for (SymbolEntry sym : symbols) {
                String symName = names.get(sym.symbolNameIndex());
                // 该符号所在节
                SectionType sectionType = sym.sectionType();
                // 符号所在节的位置信息
                PlacementInfo pInfo = placementInfo.get(sectionType);
                // 符号所在节的所在段的信息
                OutputSegment outSeg = outputSegments.get(pInfo.segmentIndex);

                // 该符号的虚拟地址
                // = 该符号所在段的虚拟地址 + 该符号所在节的段内偏移 + 该符号在目标文件该节中的偏移
                long finalAddr = outSeg.finalAddr + pInfo.offsetInSegment + sym.offset();

                // 仅对 global 符号操作
                if (sym.isGlobal()) {
                    if (globalSymbols.containsKey(symName)) {
                        throw new RuntimeException("Duplicate global symbol: " + symName);
                    }
                    globalSymbols.put(symName, new GlobalSymbol(finalAddr, i, sym.symbolNameIndex(), false));
                }

                // 记录局部符号表
                if (localSymbolMap.containsKey(symName)) {
                    throw new RuntimeException("Duplicate local symbol: " + symName);
                }
                localSymbolMap.put(symName, new LocalSymbol(finalAddr, sym.symbolNameIndex(), false));
            }
            localSymbols.put(i, localSymbolMap);
        }
        globalSymbols.put("__stack_top", new GlobalSymbol(options.stackTopVA(), -1, -1, true));

        // 重定位
        for (int i = 0; i < objects.length; i++) {
            LA64Object obj = objects[i];
            EnumMap<SectionType, PlacementInfo> placementInfo = placements.get(i);

            // 对输入文件的每一个节进行重定位
            for (Section sec : obj.sections()) {
                if (sec.relocations().isEmpty()) {
                    continue;
                }
                if (sec.type() != SectionType.TEXT) {
                    throw new RuntimeException(
                        "Relocations in section ." + sec.type().toString().toLowerCase() +
                        " are not supported yet in object " + obj.fileName());
                }

                PlacementInfo pInfo = placementInfo.get(sec.type());
                OutputSegment outSeg = outputSegments.get(pInfo.segmentIndex);
                byte[] segData = outSeg.getData();
                List<String> names = obj.symbolNames();

                // 该节起始处的虚拟地址
                long base = outSeg.finalAddr + pInfo.offsetInSegment;

                // 对该节中的每一个重定位表项
                for (RelocationEntry relocationEntry : sec.relocations()) {
                    // 在本目标文件内获取符号名
                    String symName = names.get(relocationEntry.symbolNameIndex());

                    // 目标值
                    int value;

                    // 查询符号
                    // 先查询本目标文件内符号，再查询全局符号表
                    LocalSymbol local = localSymbols.get(i).get(symName);
                    if (local != null) {
                        // 在本文件内找到
                        value = getRelocatedValue(relocationEntry, base, local.virtualAddress, local.isAbsolute);
                    } else {
                        // 未找到，在全局范围内查找
                        GlobalSymbol target = globalSymbols.get(symName);
                        if (target == null) {
                            throw new RuntimeException("Undefined symbol: " + symName);
                        }
                        value = getRelocatedValue(relocationEntry, base, target.virtualAddress, target.isAbsolute);
                    }

                    // 将 value 写入到适当位置
                    int offsetInOutSeg = pInfo.offsetInSegment + relocationEntry.offset();
                    // 目前只处理 .text 内的重定位，直接这样就好
                    patchInstruction(segData, offsetInOutSeg, relocationEntry.relocationType(), value);
                }
            }
        }

        // 构建最终输出段列表
        List<Segment> finalSegments = new ArrayList<>();
        for (OutputSegment outSeg : sortedOutputSegments) {
            byte[] data = outSeg.getData();
            Segment seg = new Segment(outSeg.finalAddr, data, outSeg.totalSize, outSeg.align, outSeg.perms);
            finalSegments.add(seg);
        }

        // 解析入口符号地址
        GlobalSymbol entrySymbol = globalSymbols.get(options.entrySymbol());
        if (entrySymbol == null) {
            throw new RuntimeException("Entry symbol not found: " + options.entrySymbol());
        }
        long entryPoint = entrySymbol.virtualAddress;

        String outFileName = objects.length == 1 ? objects[0].fileName().replace(".o", ".exe") : "a.exe";
        return new LA64Executable(
            outFileName,
            finalSegments,
            entryPoint,
            options.stackTopVA(),
            options.stackPageCount());
    }

    private int getRelocatedValue(RelocationEntry relocationEntry, long base, long targetVA, boolean isAbsolute) {
        // 需要被修补的区域的虚拟地址
        // = 所在节起始处的虚拟地址 + 该重定位项的节内偏移
        long addr = base + relocationEntry.offset();

        // 重定位符号的地址
        return switch (relocationEntry.relocationType()) {
            case R_LARCH_B21, R_LARCH_B16, R_LARCH_B26 -> {
                // PC相对偏移 = (目标地址 - 指令地址) / 4
                // 不能是绝对符号
                if (isAbsolute) {
                    throw new RuntimeException(
                        "Cannot apply PC-relative relocation to an absolute symbol: " + targetVA);
                }

                long diff = targetVA - addr;
                if ((diff & 0b11) != 0) {
                    throw new RuntimeException("Unaligned target address for PC-relative relocation: " + targetVA);
                }
                diff >>= 2;
                if (!isInteger(diff)) {
                    throw new RuntimeException("Relocation offset out of range: " + diff);
                }
                int intDiff = (int) diff;
                boolean inRange = switch (relocationEntry.relocationType()) {
                    case R_LARCH_B16 -> !BitMath.isOffs16(intDiff);
                    case R_LARCH_B21 -> !BitMath.isOffs21(intDiff);
                    case R_LARCH_B26 -> BitMath.isOffs26(intDiff);
                    default -> false; // 不可能
                };
                if (!inRange) {
                    throw new RuntimeException(
                        "Relocation offset out of range for type " + relocationEntry.relocationType() + ": " + intDiff);
                }
                yield intDiff;
            }

            // lu12i.w   rd, %abs_hi20(sym)       # R_LARCH_ABS_HI20        si20
            // ori       rd, rd, %abs_lo12(sym)   # R_LARCH_ABS_LO12        ui12
            // lu32i.d   rd, %abs64_lo20(sym)     # R_LARCH_ABS64_LO20      si20
            // lu52i.d   rd, rd, %abs64_hi12(sym) # R_LARCH_ABS64_HI12      si12
            case R_LARCH_ABS_HI20, R_LARCH_ABS_LO12, R_LARCH_ABS64_LO20, R_LARCH_ABS64_HI12 -> {
                // 只能应用于绝对符号
                if (!isAbsolute) {
                    throw new RuntimeException(
                        "Cannot apply absolute relocation to a non-absolute symbol: " + targetVA);
                }
                yield switch (relocationEntry.relocationType()) {
                    case R_LARCH_ABS_HI20 -> (int) ((targetVA >> 12) & 0xFFFFF);
                    case R_LARCH_ABS_LO12 -> (int) (targetVA & 0xFFF);
                    case R_LARCH_ABS64_LO20 -> (int) ((targetVA >> 32) & 0xFFFFF);
                    case R_LARCH_ABS64_HI12 -> (int) ((targetVA >> 52) & 0xFFF);
                    default -> 0; // 不可能
                };
            }

            // pcalau12i rd, %pc_hi20(sym)       # R_LARCH_PCALA_HI20        si20
            // addi.d    rd, rd, %pc_lo12(sym)   # R_LARCH_PCALA_LO12        si12
            // 或
            // pcalau12i rd, %pc_hi20(sym)       # R_LARCH_PCALA_HI20        si20
            // addi.d    rj, r0, %pc_lo12(sym)   # R_LARCH_PCALA_LO12        si12
            // lu32i.d   rj, %pc64_lo20(sym)     # R_LARCH_PCALA64_LO20      si20
            // lu52i.d   rj, rj, %pc64_hi12(sym) # R_LARCH_PCALA64_HI12      si12
            // add.d     rd, rd, rj
            case R_LARCH_PCALA_HI20, R_LARCH_PCALA_LO12, R_LARCH_PCALA64_LO20, R_LARCH_PCALA64_HI12 -> {
                // 只能应用于相对符号
                if (isAbsolute) {
                    throw new RuntimeException(
                        "Cannot apply PC-relative relocation to an absolute symbol: " + targetVA);
                }
                // https://github.com/llvm/llvm-project/blob/main/llvm/lib/ExecutionEngine/RuntimeDyld/RuntimeDyldELF.cpp#L818
                long pageDelta = getPageDelta(targetVA, addr, relocationEntry.relocationType());
                yield switch (relocationEntry.relocationType()) {
                    case R_LARCH_PCALA_HI20 -> (int) ((pageDelta >> 12) & 0xFFFFF);
                    case R_LARCH_PCALA_LO12 -> (int) (targetVA & 0xFFF);
                    case R_LARCH_PCALA64_LO20 -> (int) ((pageDelta >> 32) & 0xFFFFF);
                    case R_LARCH_PCALA64_HI12 -> (int) ((pageDelta >> 52) & 0xFFF);
                    default -> 0; // 不可能
                };
            }
            default -> throw new RuntimeException("Unsupported relocation type: " + relocationEntry.relocationType());
        };
    }

    private static long getPageDelta(long target, long pc, RelocationType type) {
        // https://github.com/llvm/llvm-project/blob/main/llvm/lib/ExecutionEngine/RuntimeDyld/RuntimeDyldELF.cpp#L742
        long pcalau12i_pc = switch (type) {
            case R_LARCH_PCALA64_LO20 -> pc - 8;
            case R_LARCH_PCALA64_HI12 -> pc - 12;
            default -> pc;
        };
        long result = (target & ~0xFFFL) - (pcalau12i_pc & ~0xFFFL);
        if ((target & 0x800L) != 0) {
            result += 0x1000L - 0x1_0000_0000L;
        }
        if ((result & 0x8000_0000L) != 0) {
            result += 0x1_0000_0000L;
        }
        return result;
    }

    private static void patchInstruction(byte[] code, int offset, RelocationType type, int value) {
        // 读取当前 4 字节小端指令
        int instr = ((code[offset] & 0xFF)) |
                    ((code[offset + 1] & 0xFF) << 8) |
                    ((code[offset + 2] & 0xFF) << 16) |
                    ((code[offset + 3] & 0xFF) << 24);

        int mask = 0;
        int finalValue = 0;
        switch (type) {
            case R_LARCH_B16 -> {
                // 15:0 => offs16
                // 25:10
                mask = 0xFFFF << 10;
                finalValue = value << 10;
            }
            case R_LARCH_B21 -> {
                // 15:0 + 20:16 => offs21
                // 25:10 + 4:0
                mask = (0xFFFF << 10) | 0x1F;
                finalValue = ((value & 0xFFFF) << 10) | ((value >> 16) & 0x1F);
            }
            case R_LARCH_B26 -> {
                // 15:0 + 25:16 => offs26
                // 25:10 + 9:0
                mask = 0x03FFFFFF;
                finalValue = ((value & 0xFFFF) << 10) | ((value >> 16) & 0x3FF);
            }
            case R_LARCH_ABS_HI20, R_LARCH_ABS64_LO20, R_LARCH_PCALA_HI20, R_LARCH_PCALA64_LO20 -> {
                // 19:0 => si20
                // 24:5
                mask = 0xFFFFF << 5;
                finalValue = value << 5;
            }
            case R_LARCH_ABS_LO12, R_LARCH_ABS64_HI12, R_LARCH_PCALA_LO12, R_LARCH_PCALA64_HI12 -> {
                // 11:0 => si12/ui12
                // 21:10
                mask = 0xFFF << 10;
                finalValue = value << 10;
            }
        }

        // 清除旧的立即数字段（理论上来说应该是 0），插入新值
        instr = (instr & ~mask) | (finalValue & mask);

        // 写回小端
        code[offset] = (byte) (instr & 0xFF);
        code[offset + 1] = (byte) ((instr >> 8) & 0xFF);
        code[offset + 2] = (byte) ((instr >> 16) & 0xFF);
        code[offset + 3] = (byte) ((instr >> 24) & 0xFF);
    }

    private static boolean isInteger(long value) {
        return value >= Integer.MIN_VALUE && value <= Integer.MAX_VALUE;
    }
}
