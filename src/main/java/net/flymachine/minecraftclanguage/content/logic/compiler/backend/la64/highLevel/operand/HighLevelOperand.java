package net.flymachine.minecraftclanguage.content.logic.compiler.backend.la64.highLevel.operand;

import net.flymachine.minecraftclanguage.content.logic.compiler.backend.la64.highLevel.HighLevelDataStructure;

import java.io.PrintStream;

public interface HighLevelOperand extends HighLevelDataStructure {

    @Override
    default void dumpPretty(PrintStream stream, int indentLevel, boolean indentFirstLine) {
        // 物理寄存器默认路径
        stream.append('$').append(this.toString());
    }
}
