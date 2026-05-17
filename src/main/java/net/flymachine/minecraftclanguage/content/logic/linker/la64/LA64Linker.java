package net.flymachine.minecraftclanguage.content.logic.linker.la64;

import net.flymachine.minecraftclanguage.content.logic.architecture.la64.util.BitMath;
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

    public LA64Executable link(LA64Object... objects) {
        if (objects.length == 0) {
            throw new IllegalArgumentException("Cannot link an empty object");
        }

        // 每节的起始偏移，相对与 text 节起点处
        // data 节放置在 text 节后的下一个页开始，bss 节放置在 data 节后的下一个页开始
        Map<SectionType, List<Integer>> sectionsOffsets = new EnumMap<>(SectionType.class);
        sectionsOffsets.put(SectionType.TEXT, new ArrayList<>());
        sectionsOffsets.put(SectionType.DATA, new ArrayList<>());
        sectionsOffsets.put(SectionType.BSS, new ArrayList<>());

        // 合并 text 节，记录每个 text 节的起始偏移
        List<Integer> textOffsets = sectionsOffsets.get(SectionType.TEXT);
        ByteArrayOutputStream mergedTextStream = new ByteArrayOutputStream();
        for (LA64Object obj : objects) {
            textOffsets.add(mergedTextStream.size());
            try {
                byte[] data = obj.sections().stream().filter(sec -> sec.type() == SectionType.TEXT).findFirst()
                                 .map(Section::data).orElse(new byte[0]);
                mergedTextStream.write(data);
            } catch (IOException e) {
                throw new RuntimeException("Failed to merge text sections", e);
            }
        }
        byte[] mergedText = mergedTextStream.toByteArray();

        int dataBaseOffset = BitMath.alignUp(mergedText.length, 4096);
        // 合并 data 节，记录每个 data 节的起始偏移
        List<Integer> dataOffsets = sectionsOffsets.get(SectionType.DATA);
        ByteArrayOutputStream mergedDataStream = new ByteArrayOutputStream();
        for (LA64Object obj : objects) {
            dataOffsets.add(dataBaseOffset + mergedDataStream.size());
            try {
                byte[] data = obj.sections().stream().filter(sec -> sec.type() == SectionType.DATA).findFirst()
                                 .map(Section::data).orElse(new byte[0]);
                mergedDataStream.write(data);
            } catch (IOException e) {
                throw new RuntimeException("Failed to merge data sections", e);
            }
        }
        byte[] mergedData = mergedDataStream.toByteArray();

        int bssBaseOffset = BitMath.alignUp(dataBaseOffset + mergedData.length, 4096);
        // 合并 bss 节，记录每个 bss 节的起始偏移
        List<Integer> bssOffsets = sectionsOffsets.get(SectionType.BSS);
        int totalBssSize = 0;
        for (LA64Object obj : objects) {
            bssOffsets.add(bssBaseOffset + totalBssSize);
            Section bssSection =
                obj.sections().stream().filter(sec -> sec.type() == SectionType.BSS).findFirst().orElse(null);
            if (bssSection != null) {
                totalBssSize += bssSection.size();
            }
        }

        // 构建全局符号表与各文件的局部符号表
        Map<String, GlobalSymbol> globalSymbols = new HashMap<>();
        Map<Integer, Map<String, LocalSymbol>> localSymbols = new HashMap<>();
        for (int i = 0; i < objects.length; i++) {
            LA64Object obj = objects[i];

            List<SymbolEntry> symbols = obj.symbols();
            List<String> names = obj.symbolNames();

            Map<String, LocalSymbol> localSymbolMap = new HashMap<>();

            for (SymbolEntry sym : symbols) {
                String symName = names.get(sym.symbolNameIndex());

                // 该符号的相对虚拟地址，相对于 text 节起始
                // = 该符号所在节的偏移 + 该符号在目标文件该节中的偏移
                long finalAddr = sectionsOffsets.get(sym.sectionType()).get(i) + sym.offset();

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
            // 该目标文件 text 节的起始偏移
            int base = textOffsets.get(i);

            List<RelocationEntry> relocationEntries =
                obj.sections().stream()
                   .filter(sec -> sec.type() == SectionType.TEXT).findFirst()
                   .map(Section::relocations).orElse(List.of());
            
            List<String> names = obj.symbolNames();

            for (RelocationEntry relocationEntry : relocationEntries) {
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

                // 将 value 写入到 text 的适当位置
                int offsetInMerged = base + relocationEntry.offset();
                patchInstruction(mergedText, offsetInMerged, relocationEntry.relocationType(), value);
            }
        }

        // 解析入口符号偏移
        GlobalSymbol entrySymbol = globalSymbols.get(options.entrySymbol());
        if (entrySymbol == null) {
            throw new RuntimeException("Entry symbol not found: " + options.entrySymbol());
        }
        int entryOffset = (int) (entrySymbol.virtualAddress);

        String outFileName = objects.length == 1 ? objects[0].fileName().replace(".o", ".exe") : "a.exe";
        return new LA64Executable(
            outFileName, mergedText, options.textVA(), mergedData, totalBssSize, entryOffset, options.stackTopVA(),
            options.stackPageCount());
    }

    private int getRelocatedValue(RelocationEntry relocationEntry, int base, long targetVA, boolean isAbsolute) {
        // 需要被修补的指令的相对地址，相对于 text 节起始
        // = 该目标文件 text 节偏移 + 该重定位项指令在目标文件 text 节的偏移
        long instrAddr = base + relocationEntry.offset();

        // 重定位符号的地址
        return switch (relocationEntry.relocationType()) {
            case R_LARCH_B21, R_LARCH_B16, R_LARCH_B26 -> {
                // PC相对偏移 = (目标地址 - 指令地址) / 4
                // 不能是绝对符号
                if (isAbsolute) {
                    throw new RuntimeException(
                        "Cannot apply PC-relative relocation to an absolute symbol: " + targetVA);
                }

                long diff = targetVA - instrAddr;
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
                long pageDelta = getPageDelta(targetVA, instrAddr, relocationEntry.relocationType());
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
