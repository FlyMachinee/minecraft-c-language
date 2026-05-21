package net.flymachine.minecraftclanguage.content.logic.compiler.frontend.c99.ast.node;

import net.flymachine.minecraftclanguage.content.logic.compiler.frontend.c99.ast.AstVisitor;
import net.flymachine.minecraftclanguage.content.logic.errorHandle.SourceLocation;

import java.io.PrintStream;

public interface AstInterface {
    SourceLocation getWholeLocation();

    <T> T accept(AstVisitor<T> visitor);

    /**
     * 将 AST 节点以格式化的方式输出到流中
     * <p>
     * 输出完毕时，不会输出换行符或者分隔符
     *
     * @param stream          输出流
     * @param indentLevel     当前 AST 节点的缩进级别
     * @param indentFirstLine 是否要在第一行前面添加缩进
     */
    void dump(PrintStream stream, int indentLevel, boolean indentFirstLine);

    default void dump(PrintStream stream) {
        dump(stream, 0, false);
    }
}

