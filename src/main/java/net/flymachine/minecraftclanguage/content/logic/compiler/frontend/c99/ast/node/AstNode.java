package net.flymachine.minecraftclanguage.content.logic.compiler.frontend.c99.ast.node;

public interface AstNode {
    void genFormattedString(StringBuilder stringBuilder, int indentLevel, boolean indentFirstLine);

    default void genFormattedString(StringBuilder stringBuilder) {
        genFormattedString(stringBuilder, 0, false);
    }
}
