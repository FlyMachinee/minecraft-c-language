package net.flymachine.minecraftclanguage.content.logic.compiler.backend.la64.highLevel.operand;

/**
 * 代表着栈上内存位置
 *
 * @param offset     相对与栈帧底部（fp）的偏移量，栈向地址小的方向生长
 * @param fpRelative 是否使用 fp 寻址，{@code true} 则相对于 fp 偏移，{@code false} 则相对于 sp 偏移
 */
public record Stack(int offset, boolean fpRelative) implements HighLevelOperand {

    public Stack(int offset) {
        this(offset, true);
    }
}
