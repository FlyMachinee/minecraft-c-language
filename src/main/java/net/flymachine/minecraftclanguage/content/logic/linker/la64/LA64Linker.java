package net.flymachine.minecraftclanguage.content.logic.linker.la64;

import net.flymachine.minecraftclanguage.content.logic.architecture.la64.util.BitMath;
import net.flymachine.minecraftclanguage.content.logic.executable.la64.LA64Executable;
import net.flymachine.minecraftclanguage.content.logic.linker.LinkOptions;
import net.flymachine.minecraftclanguage.content.logic.memory.Segment;
import net.flymachine.minecraftclanguage.content.logic.object.SymbolEntry;
import net.flymachine.minecraftclanguage.content.logic.object.la64.LA64Object;
import net.flymachine.minecraftclanguage.content.logic.object.la64.RelocationEntry;
import net.flymachine.minecraftclanguage.content.logic.object.la64.RelocationType;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class LA64Linker {

    public LA64Linker() { }

    private LinkOptions options = LinkOptions.DEFAULT;

    public void setOptions(LinkOptions options) {
        this.options = options;
    }

    private static class GlobalSymbol {
        long virtualAddress;
        int objectIndex;
        int offsetInObject;
        boolean isAbsolute; // 表示该符号的地址是否为绝对地址

        public GlobalSymbol(long virtualAddress, int objectIndex, int offsetInObject, boolean isAbsolute) {
            this.virtualAddress = virtualAddress;
            this.objectIndex = objectIndex;
            this.offsetInObject = offsetInObject;
            this.isAbsolute = isAbsolute;
        }
    }

    public LA64Executable link(LA64Object... objects) {
        if (objects.length == 0) {
            throw new IllegalArgumentException("Cannot link an empty object");
        }

        // 合并 text 段，记录每个 text 段的起始偏移
        List<Integer> baseOffsets = new ArrayList<>();
        ByteArrayOutputStream mergedTextStream = new ByteArrayOutputStream();
        for (LA64Object obj : objects) {
            baseOffsets.add(mergedTextStream.size());
            try {
                mergedTextStream.write(obj.text());
            } catch (IOException e) {
                throw new RuntimeException("Failed to merge text sections", e);
            }
        }
        byte[] mergedText = mergedTextStream.toByteArray();


        // 构建全局符号表
        Map<String, GlobalSymbol> globalSymbols = new HashMap<>();
        for (int i = 0; i < objects.length; i++) {
            LA64Object obj = objects[i];
            // 该目标文件 text 段的起始偏移
            int base = baseOffsets.get(i);

            List<SymbolEntry> symbols = obj.symbols();
            List<String> names = obj.symbolNames();

            for (SymbolEntry sym : symbols) {
                // 仅对 global 符号操作
                if (sym.isGlobal()) {
                    String symName = names.get(sym.symbolNameIndex());
                    if (globalSymbols.containsKey(symName)) {
                        throw new RuntimeException("Duplicate global symbol: " + symName);
                    }
                    if (sym.segment() != Segment.TEXT) {
                        throw new RuntimeException("Only TEXT segment supported currently");
                    }
                    // 该符号的相对虚拟地址，相对于 text 段起始
                    // = 该目标文件 text 段偏移 + 该符号在目标文件 text 段的偏移
                    long finalAddr = base + sym.offset();
                    globalSymbols.put(symName, new GlobalSymbol(finalAddr, i, sym.offset(), false));
                }
            }
        }
        globalSymbols.put("__stack_top", new GlobalSymbol(options.stackTopVA(), -1, -1, true));

        // 重定位
        for (int i = 0; i < objects.length; i++) {
            LA64Object obj = objects[i];
            // 该目标文件 text 段的起始偏移
            int base = baseOffsets.get(i);

            List<RelocationEntry> relocationEntries = obj.relocations();
            List<String> names = obj.symbolNames();

            for (RelocationEntry relocationEntry : relocationEntries) {
                // 在本目标文件内获取符号名
                String symName = names.get(relocationEntry.symbolNameIndex());

                // 查询符号
                GlobalSymbol target = globalSymbols.get(symName);
                if (target == null) {
                    throw new RuntimeException("Undefined symbol: " + symName);
                }

                // 获取重定位后符号的目标值
                int value = getRelocatedValue(relocationEntry, base, target);

                // 将 value 写入到 text 的适当位置
                int offsetInMerged = base + relocationEntry.textOffset();
                patchInstruction(mergedText, offsetInMerged, relocationEntry.relocationType(), value);
            }
        }

        // 解析入口符号偏移
        long entryAddr = -1;
        for (Map.Entry<String, GlobalSymbol> entry : globalSymbols.entrySet()) {
            if (entry.getKey().equals(options.entrySymbol())) {
                entryAddr = entry.getValue().virtualAddress;
                break;
            }
        }
        if (entryAddr == -1) {
            throw new RuntimeException("Entry symbol not found: " + options.entrySymbol());
        }
        int entryOffset = (int) (entryAddr);

        String outFileName = objects.length == 1 ? objects[0].fileName().replace(".o", ".exe") : "a.exe";
        return new LA64Executable(
            outFileName, mergedText, options.textVA(), entryOffset, options.stackTopVA(), options.stackPageCount());
    }

    private int getRelocatedValue(RelocationEntry relocationEntry, int base, GlobalSymbol target) {
        // 需要被修补的指令的相对地址，相对于 text 段起始
        // = 该目标文件 text 段偏移 + 该重定位项指令在目标文件 text 段的偏移
        long instrAddr = base + relocationEntry.textOffset();

        // 重定位符号的地址
        long targetAddr = target.virtualAddress;
        return switch (relocationEntry.relocationType()) {
            case R_LARCH_B21, R_LARCH_B16, R_LARCH_B26 -> {
                // PC相对偏移 = (目标地址 - 指令地址) / 4
                // 不能是绝对符号
                if (target.isAbsolute) {
                    throw new RuntimeException(
                        "Cannot apply PC-relative relocation to an absolute symbol: " + target.virtualAddress);
                }

                long diff = targetAddr - instrAddr;
                if ((diff & 0b11) != 0) {
                    throw new RuntimeException("Unaligned target address for PC-relative relocation: " + targetAddr);
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
                if (!target.isAbsolute) {
                    throw new RuntimeException(
                        "Cannot apply absolute relocation to a non-absolute symbol: " + target.virtualAddress);
                }
                yield switch (relocationEntry.relocationType()) {
                    case R_LARCH_ABS_HI20 -> (int) ((targetAddr >> 12) & 0xFFFFF);
                    case R_LARCH_ABS_LO12 -> (int) (targetAddr & 0xFFF);
                    case R_LARCH_ABS64_LO20 -> (int) ((targetAddr >> 32) & 0xFFFFF);
                    case R_LARCH_ABS64_HI12 -> (int) ((targetAddr >> 52) & 0xFFF);
                    default -> 0; // 不可能
                };
            }
        };
    }

    private static void patchInstruction(byte[] code, int offset, RelocationType type, int value) {
        // 读取当前4字节小端指令
        int instr = ((code[offset] & 0xFF)) |
                    ((code[offset + 1] & 0xFF) << 8) |
                    ((code[offset + 2] & 0xFF) << 16) |
                    ((code[offset + 3] & 0xFF) << 24);

        int mask = 0;
        int finalValue = 0;
        switch (type) {
            case R_LARCH_B16 -> {
                // 15:0 =>
                // 25:10
                mask = 0xFFFF << 10;
                finalValue = value << 10;
            }
            case R_LARCH_B21 -> {
                // 15:0 + 20:16 =>
                // 25:10 + 4:0
                mask = (0xFFFF << 10) | 0x1F;
                finalValue = ((value & 0xFFFF) << 10) | ((value >> 16) & 0x1F);
            }
            case R_LARCH_B26 -> {
                // 15:0 + 25:16 =>
                // 25:10 + 9:0
                mask = 0x03FFFFFF;
                finalValue = ((value & 0xFFFF) << 10) | ((value >> 16) & 0x3FF);
            }
            case R_LARCH_ABS_HI20, R_LARCH_ABS64_LO20 -> {
                // 19:0 =>
                // 24:5
                mask = 0xFFFFF << 5;
                finalValue = value << 5;
            }
            case R_LARCH_ABS_LO12, R_LARCH_ABS64_HI12 -> {
                // 11:0 =>
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
