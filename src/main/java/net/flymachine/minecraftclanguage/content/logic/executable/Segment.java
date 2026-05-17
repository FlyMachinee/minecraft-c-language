package net.flymachine.minecraftclanguage.content.logic.executable;

import org.jetbrains.annotations.Nullable;

import java.util.Set;

/**
 * 可执行文件的段
 *
 * @param virtualAddr 段起点虚拟地址
 * @param data        段数据，可能为 null（表示未初始化数据段）
 * @param size        段大小，单位字节
 * @param align       段对齐要求，单位字节
 * @param perms       段权限
 */
public record Segment(long virtualAddr, byte @Nullable [] data, long size, int align, Set<SegmentPermission> perms) {

    public Segment {
        if (data == null && size == 0) {
            throw new IllegalArgumentException("Empty segment is not allowed");
        }
        if (data != null && size < data.length) {
            size = data.length;
        }
        align = Math.max(align, 1);
    }
}

