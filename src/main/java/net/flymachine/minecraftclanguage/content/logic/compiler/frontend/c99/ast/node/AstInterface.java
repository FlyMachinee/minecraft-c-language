package net.flymachine.minecraftclanguage.content.logic.compiler.frontend.c99.ast.node;

import net.flymachine.minecraftclanguage.content.logic.compiler.frontend.c99.ast.AstVisitor;
import net.flymachine.minecraftclanguage.content.logic.errorHandle.SourceLocation;

public interface AstInterface {
    SourceLocation getWholeLocation();

    <T> T accept(AstVisitor<T> visitor);

    void genFormattedString(StringBuilder stringBuilder, int indentLevel, boolean indentFirstLine);

    default void genFormattedString(StringBuilder stringBuilder) {
        genFormattedString(stringBuilder, 0, false);
    }
}
