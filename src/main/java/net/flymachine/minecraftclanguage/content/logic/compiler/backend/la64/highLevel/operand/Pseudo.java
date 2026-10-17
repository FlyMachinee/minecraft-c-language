package net.flymachine.minecraftclanguage.content.logic.compiler.backend.la64.highLevel.operand;

import java.io.PrintStream;

/**
 * 伪寄存器，最终将会被替换为物理寄存器或栈上内存
 *
 * @param name
 */
public record Pseudo(String name) implements HighLevelOperand {
    @Override
    public void dumpPretty(PrintStream stream, int indentLevel, boolean indentFirstLine) {
        stream.append('%').append(name);
    }
}
