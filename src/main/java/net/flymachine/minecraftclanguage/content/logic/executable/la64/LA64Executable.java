package net.flymachine.minecraftclanguage.content.logic.executable.la64;

import net.flymachine.minecraftclanguage.content.logic.executable.Segment;
import net.flymachine.minecraftclanguage.content.logic.executable.SegmentPermission;

import java.io.PrintStream;
import java.util.List;

/**
 * LA64 架构下的可执行文件
 *
 * @param fileName       可执行文件名
 * @param segments       可执行文件包含的段列表
 * @param entryPoint     可执行文件的入口虚拟地址
 * @param stackTopVA     栈顶虚拟地址
 * @param stackPageCount 栈占用的页数
 */
public record LA64Executable(
    String fileName, List<Segment> segments, long entryPoint, long stackTopVA, int stackPageCount) {

    public void dump(PrintStream out) {
        out.println("File: " + fileName);
        out.println();

        out.println("Segments:");
        out.printf(
            "  %-5s %-13s %-12s %-8s %-6s %s%n",
            "Index", "VirtualAddr", "Size(bytes)", "DataLen", "Align", "Permissions");
        int idx = 0;
        for (Segment seg : segments) {
            String sizeStr = String.valueOf(seg.size());
            String dataLenStr;
            if (seg.data() == null) {
                dataLenStr = "-";
            } else {
                dataLenStr = String.valueOf(seg.data().length);
            }
            String alignStr = String.valueOf(seg.align());
            StringBuilder permsStr = new StringBuilder();
            for (SegmentPermission perm : SegmentPermission.values()) {
                if (seg.perms().contains(perm)) {
                    permsStr.append(switch (perm) {
                        case READ -> "R";
                        case WRITE -> "W";
                        case EXEC -> "X";
                    });
                } else {
                    permsStr.append(' ');
                }
            }

            out.printf(
                "  %-5d 0x%-11x %-12s %-8s %-6s %s%n",
                idx, seg.virtualAddr(), sizeStr, dataLenStr, alignStr, permsStr);
            idx++;
        }
        out.println();

        out.printf("Entry point: 0x%x%n", entryPoint);
        out.printf("Stack top: 0x%x, stack pages: %d%n", stackTopVA, stackPageCount);
    }
}
