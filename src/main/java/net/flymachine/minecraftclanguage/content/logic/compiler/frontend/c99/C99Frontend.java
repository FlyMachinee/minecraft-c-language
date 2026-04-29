package net.flymachine.minecraftclanguage.content.logic.compiler.frontend.c99;

import net.flymachine.minecraftclanguage.content.logic.compiler.frontend.antlr.C99Lexer;
import net.flymachine.minecraftclanguage.content.logic.compiler.frontend.antlr.C99Parser;
import net.flymachine.minecraftclanguage.content.logic.compiler.frontend.c99.ast.AstBuilderVisitor;
import net.flymachine.minecraftclanguage.content.logic.compiler.frontend.c99.ast.AstToTacLowerer;
import net.flymachine.minecraftclanguage.content.logic.compiler.frontend.c99.ast.node.AstNode;
import net.flymachine.minecraftclanguage.content.logic.compiler.frontend.c99.ast.node.ProgramNode;
import net.flymachine.minecraftclanguage.content.logic.compiler.ir.TacProgram;
import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CommonTokenStream;

import javax.annotation.Nullable;

public final class C99Frontend {

    public C99Frontend() { }

    /**
     * 将指定的字符流编译成中间表示
     *
     * @param charStream 包含源文件内容的字符流
     * @return 前端编译后的中间表示，如果遇到错误则返回 null
     */
    public @Nullable TacProgram compile(CharStream charStream) {
        C99Lexer lexer = new C99Lexer(charStream);
        lexer.removeErrorListeners();
        LexerErrorListener lexerErrorListener = new LexerErrorListener();
        lexer.addErrorListener(lexerErrorListener);

        C99Parser parser = new C99Parser(new CommonTokenStream(lexer));
        parser.removeErrorListeners();
        ParserErrorListener parserErrorListener = new ParserErrorListener();
        parser.addErrorListener(parserErrorListener);

        C99Parser.CompilationUnitContext tree = parser.compilationUnit();

        if (lexerErrorListener.hasErrors() || parser.getNumberOfSyntaxErrors() > 0) {
            return null;
        }

        AstNode ast = new AstBuilderVisitor().visit(tree);
        return new AstToTacLowerer().lower((ProgramNode) ast);
    }
}
