package net.flymachine.minecraftclanguage.content.logic.linker;

import net.flymachine.minecraftclanguage.content.logic.executable.SegmentPermission;
import net.flymachine.minecraftclanguage.content.logic.object.SectionType;

import java.util.List;
import java.util.Set;

/**
 * 链接选项
 *
 * @param entrySymbol     可执行文件的入口符号名
 * @param stackTopVA      栈顶的虚拟地址
 * @param stackPageCount  栈占用的页数
 * @param segmentMappings 段映射配置列表，定义如何将节映射到段
 * @param segmentConfigs  段配置列表，定义段的虚拟地址和权限等属性
 */
public record LinkOptions(
    String entrySymbol,
    long stackTopVA,
    int stackPageCount,
    List<SegmentMapping> segmentMappings,
    List<SegmentConfig> segmentConfigs
) {


    public LinkOptions {
        if (segmentMappings.isEmpty()) {
            throw new IllegalArgumentException("segmentMappings must not be empty");
        }
        if (segmentConfigs.isEmpty()) {
            throw new IllegalArgumentException("segmentConfigs must not be empty");
        }
    }

    public static final LinkOptions DEFAULT = new LinkOptions(
        "_start",
        0x00007ffffffffff0L,
        4,
        List.of(
            new SegmentMapping(SectionType.TEXT, 0),
            new SegmentMapping(SectionType.DATA, 1),
            new SegmentMapping(SectionType.BSS, 1),
            new SegmentMapping(SectionType.RODATA, 0)),
        List.of(
            new SegmentConfig(0, 0x80000000L, Set.of(SegmentPermission.READ, SegmentPermission.EXEC)),
            new SegmentConfig(1, 0, Set.of(SegmentPermission.READ, SegmentPermission.WRITE)))
    );

    /**
     * 段映射配置
     *
     * @param sectionType  要映射的节类型
     * @param segmentIndex 要合并到第几个段（从 0 开始计数）
     */
    public record SegmentMapping(
        SectionType sectionType,
        int segmentIndex
    ) { }

    /**
     * 段配置
     *
     * @param segmentIndex 段索引，必须与 SegmentMapping 中的 segmentIndex 对应
     * @param virtualAddr  段的起始虚拟地址，0 表示自动分配（紧随前一非零段后并按页对齐）
     * @param permissions  段权限，至少包含 READ 权限，EXEC 权限表示可执行，WRITE 权限表示可写
     */
    public record SegmentConfig(
        int segmentIndex,
        long virtualAddr,
        Set<SegmentPermission> permissions
    ) { }
}
