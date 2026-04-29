package net.flymachine.minecraftclanguage.content.logic.compiler.backend.la64.highLevel.operand;

/**
 * 伪寄存器，最终将会被替换为物理寄存器或栈上内存
 *
 * @param identifier
 */
public record Pseudo(String identifier) implements HighLevelOperand { }
