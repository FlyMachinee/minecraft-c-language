package net.flymachine.minecraftclanguage.content.logic.compiler.backend.la64.highLevel.instruction;

import net.flymachine.minecraftclanguage.content.logic.compiler.backend.la64.highLevel.AsmType;
import net.flymachine.minecraftclanguage.content.logic.compiler.backend.la64.highLevel.HighLevelDataStructure;
import net.flymachine.minecraftclanguage.content.logic.compiler.backend.la64.highLevel.operand.HighLevelOperand;

import java.io.PrintStream;

public interface HighLevelInstruction extends HighLevelDataStructure {
    <T> T accept(HighLevelVisitor<T> visitor);

    default void dumpBinaryLike(
        PrintStream stream, HighLevelOperand dst, HighLevelOperand lhs, HighLevelOperand rhs,
        String op, AsmType type) {

        dst.dumpPretty(stream);
        stream.print(" = ");
        lhs.dumpPretty(stream);
        stream.append(' ').append(op).append(' ');
        rhs.dumpPretty(stream);
        stream.print(" [");
        stream.print(type);
        stream.print("]");
    }
}
