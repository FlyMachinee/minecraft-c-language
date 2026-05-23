package net.flymachine.minecraftclanguage.content.logic.compiler.ir;

import java.io.PrintStream;

/**
 * TAC 为三地址码 (Three-Address Code)
 */
public interface TacDataStructure {
    void dump(PrintStream stream, int indentLevel, boolean indentFirstLine);

    default void dump(PrintStream stream) {
        dump(stream, 0, false);
    }
}
