package net.flymachine.minecraftclanguage.content.logic.compiler.backend.la64.highLevel.operand;

/**
 * 伪内存地址，最终将会被替换为实际的内存
 *
 * @param name   指代的对象标识
 * @param offset 偏移量
 */
public record PseudoMemory(String name, long offset) implements HighLevelOperand {
}
