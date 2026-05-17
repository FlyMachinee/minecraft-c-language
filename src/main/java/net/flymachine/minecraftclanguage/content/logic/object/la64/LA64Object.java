package net.flymachine.minecraftclanguage.content.logic.object.la64;

import net.flymachine.minecraftclanguage.content.logic.object.RelocationEntry;
import net.flymachine.minecraftclanguage.content.logic.object.Section;
import net.flymachine.minecraftclanguage.content.logic.object.SectionType;
import net.flymachine.minecraftclanguage.content.logic.object.SymbolEntry;

import java.io.PrintStream;
import java.util.List;

/**
 * LA64 架构下的目标文件
 *
 * @param fileName    文件名
 * @param sections    节列表，每个节包含数据和重定位表
 * @param symbols     符号表
 * @param symbolNames 符号名列表，符号表项、重定位表项中的 symbolNameIndex 字段是该列表的索引
 */
public record LA64Object(
    String fileName,
    List<Section> sections,
    List<SymbolEntry> symbols,
    List<String> symbolNames) {

    public void dump(PrintStream out) {
        out.println("File: " + fileName);
        out.println();

        // 打印每个节的基本信息
        out.println("Sections:");
        out.printf("  %-10s %-12s %-8s %s%n", "Name", "Size(bytes)", "DataLen", "Relocs");
        for (Section sec : sections) {
            String secName = "." + sec.type().name().toLowerCase();
            String sizeStr = String.valueOf(sec.size());
            String dataLenStr;
            if (sec.type() == SectionType.BSS) {
                dataLenStr = "-";
            } else {
                assert sec.data() != null;
                dataLenStr = String.valueOf(sec.data().length);
            }
            String relocsStr = sec.relocations().isEmpty() ? "-" : String.valueOf(sec.relocations().size());
            out.printf("  %-10s %-12s %-8s %s%n", secName, sizeStr, dataLenStr, relocsStr);
        }
        out.println();

        // 打印重定位信息
        for (Section sec : sections) {
            List<RelocationEntry> relocs = sec.relocations();
            if (relocs.isEmpty()) { continue; }
            String secName = sec.type().name().toLowerCase();
            out.println("Relocations in ." + secName + ":");
            out.printf("  %-8s  %-24s %s%n", "Offset", "Type", "Symbol");
            for (RelocationEntry rel : relocs) {
                String symName = symbolNames.get(rel.symbolNameIndex());
                out.printf("  %08x  %-24s %s%n", rel.offset(), rel.relocationType().name(), symName);
            }
            out.println();
        }

        // 打印符号表
        if (!symbols.isEmpty()) {
            out.println("Symbols:");
            out.printf("  %-5s %-8s %-10s %-6s %s%n", "Index", "Section", "Offset", "Global", "Name");
            int idx = 0;
            for (SymbolEntry sym : symbols) {
                String secName = sym.sectionType().name().toLowerCase();
                String globalFlag = sym.isGlobal() ? "yes" : "no";
                String symName = symbolNames.get(sym.symbolNameIndex());
                out.printf(
                    "  %-5d .%-7s 0x%-8x %-6s %s%n",
                    idx, secName, sym.offset(), globalFlag, symName);
                idx++;
            }
            out.println();
        } else {
            out.println("No symbols.");
        }
    }
}

// TODO: 尝试为不同架构共用 Object 类