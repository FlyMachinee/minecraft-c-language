package net.flymachine.minecraftclanguage.content.logic.compiler.backend.la64.highLevel.operand;

/**
 * 数据标识符，标识着 data 段或 bss 段上的某个符号
 *
 * @param name   符号名
 * @param offset 偏移量
 */
public record Data(String name, long offset) implements HighLevelOperand {

    public Data(String name) {
        this(name, 0);
    }
}
