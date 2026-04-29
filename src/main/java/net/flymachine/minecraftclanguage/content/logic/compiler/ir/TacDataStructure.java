package net.flymachine.minecraftclanguage.content.logic.compiler.ir;

/**
 * TAC 为三地址码 (Three-Address Code)
 */
public interface TacDataStructure {
    void genFormattedString(StringBuilder stringBuilder, int indentLevel, boolean indentFirstLine);

    default void genFormattedString(StringBuilder stringBuilder) {
        genFormattedString(stringBuilder, 0, false);
    }
}
