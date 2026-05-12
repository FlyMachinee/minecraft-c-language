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

        public GlobalSymbol(long virtualAddress, int objectIndex, int offsetInObject) {
            this.virtualAddress = virtualAddress;
            this.objectIndex = objectIndex;
            this.offsetInObject = offsetInObject;
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
                    // 该符号的绝对虚拟地址
                    // 最终 text 段起始地址 + 该目标文件 text 段偏移 + 该符号在目标文件 text 段的偏移
                    long finalAddr = options.textVA() + base + sym.offset();
                    globalSymbols.put(symName, new GlobalSymbol(finalAddr, i, sym.offset()));
                }
            }
        }


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
        int entryOffset = (int) (entryAddr - options.textVA());

        String outFileName = objects.length == 1 ? objects[0].fileName().replace(".o", ".exe") : "a.exe";
        return new LA64Executable(outFileName, mergedText, options.textVA(), entryOffset);
    }

    private int getRelocatedValue(RelocationEntry relocationEntry, int base, GlobalSymbol target) {
        // 需要被修补的指令的绝对地址
        // = 最终 text 段起始地址 + 该目标文件 text 段偏移 + 该重定位项指令在目标文件 text 段的偏移
        long instrAddr = options.textVA() + base + relocationEntry.textOffset();

        // 重定位符号的绝对地址
        long targetAddr = target.virtualAddress;
        return switch (relocationEntry.relocationType()) {
            case OFFS21_PC_REL, OFFS16_PC_REL, OFFS26_PC_REL -> {
                // PC相对偏移 = (目标地址 - 指令地址) / 4
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
                    case OFFS16_PC_REL -> !BitMath.isOffs16(intDiff);
                    case OFFS21_PC_REL -> !BitMath.isOffs21(intDiff);
                    case OFFS26_PC_REL -> BitMath.isOffs26(intDiff);
                };
                if (!inRange) {
                    throw new RuntimeException(
                        "Relocation offset out of range for type " + relocationEntry.relocationType() + ": " + intDiff);
                }
                yield intDiff;
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
            case OFFS16_PC_REL -> {
                // 15:0 =>
                // 25:10
                mask = 0xFFFF << 10;
                finalValue = value << 10;
            }
            case OFFS21_PC_REL -> {
                // 15:0 + 20:16 =>
                // 25:10 + 4:0
                mask = (0xFFFF << 10) | 0x1F;
                finalValue = ((value & 0xFFFF) << 10) | ((value >> 16) & 0x1F);
            }
            case OFFS26_PC_REL -> {
                // 15:0 + 25:16 =>
                // 25:10 + 9:0
                mask = 0x03FFFFFF;
                finalValue = ((value & 0xFFFF) << 10) | ((value >> 16) & 0x3FF);
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
