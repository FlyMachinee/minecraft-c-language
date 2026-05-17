package net.flymachine.minecraftclanguage.content.logic.object;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * 表示一个节（Section），如 .text .data .bss
 *
 * @param type        节类型
 * @param data        节已初始化数据（对于 .bss 节，data 为 null）
 * @param size        节长度（data.length，对于 .bss 节，size 表示预留空间大小）
 * @param align       节对齐要求，单位字节
 * @param relocations 该节内部的重定位表项列表
 */
public record Section(
    SectionType type, byte @Nullable [] data, int size, int align, @NotNull List<RelocationEntry> relocations) {

    public Section {
        if (type == SectionType.BSS && data != null) {
            throw new IllegalArgumentException(".bss section must not have data");
        }
        if (type != SectionType.BSS && data == null) {
            throw new IllegalArgumentException("Sections except .bss must have data");
        }
        if (type == SectionType.BSS) {
            if (!relocations.isEmpty()) {
                throw new IllegalArgumentException(".bss section must not have relocation entries");
            }
        } else {
            if (size != data.length) {
                size = data.length; // 确保 size 与 data 长度一致
            }
        }
        align = Math.max(align, 1);
    }

    public Section(SectionType type, byte @NotNull [] data, int align, @NotNull List<RelocationEntry> relocations) {
        this(type, data, data.length, align, relocations);
    }

    public Section(SectionType type, int size, int align) {
        this(type, null, size, align, List.of());
    }
}
