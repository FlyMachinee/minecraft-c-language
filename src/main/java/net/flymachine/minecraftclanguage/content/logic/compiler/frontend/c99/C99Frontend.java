package net.flymachine.minecraftclanguage.content.logic.compiler.frontend.c99;

import net.flymachine.minecraftclanguage.content.logger.ConsoleLogger;
import net.flymachine.minecraftclanguage.content.logger.Logger;
import net.flymachine.minecraftclanguage.content.logic.compiler.frontend.antlr.C99Lexer;
import net.flymachine.minecraftclanguage.content.logic.compiler.frontend.antlr.C99Parser;
import net.flymachine.minecraftclanguage.content.logic.compiler.frontend.c99.ast.node.AstNode;
import net.flymachine.minecraftclanguage.content.logic.compiler.frontend.c99.ast.node.ProgramNode;
import net.flymachine.minecraftclanguage.content.logic.compiler.ir.TacProgram;
import net.flymachine.minecraftclanguage.content.logic.errorHandle.DiagnosticReporter;
import net.flymachine.minecraftclanguage.content.logic.errorHandle.SourceFile;
import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CommonTokenStream;

import javax.annotation.Nullable;

public final class C99Frontend {

    public C99Frontend() { }

    /**
     * 将指定的字符流编译成中间表示
     *
     * @param charStream 包含源文件内容的字符流
     * @return 前端编译后的中间表示以及符号表，如果遇到错误则返回 null
     */
    public @Nullable Result compile(CharStream charStream) {
        Logger logger = new ConsoleLogger();
        SourceFile sourceFile = new SourceFile(charStream);
        DiagnosticReporter reporter = new DiagnosticReporter(logger, sourceFile);

        C99Lexer lexer = new C99Lexer(charStream);
        lexer.removeErrorListeners();
        LexerErrorListener lexerErrorListener = new LexerErrorListener(reporter);
        lexer.addErrorListener(lexerErrorListener);

        C99Parser parser = new C99Parser(new CommonTokenStream(lexer));
        parser.removeErrorListeners();
        ParserErrorListener parserErrorListener = new ParserErrorListener(reporter);
        parser.addErrorListener(parserErrorListener);

        C99Parser.CompilationUnitContext tree = parser.compilationUnit();
        if (reporter.getErrorCount() > 0) {
            return null;
        }

        AstBuilderVisitor astBuilderVisitor = new AstBuilderVisitor(reporter);
        AstNode ast = astBuilderVisitor.visit(tree);

        if (ast == null || reporter.getErrorCount() > 0) {
            return null;
        }

        IdentifierResolutionPass identifierResolutionPass = new IdentifierResolutionPass(reporter);
        ast.accept(identifierResolutionPass);

        if (reporter.getErrorCount() > 0) {
            return null;
        }

        TypeCheckingPass typeCheckingPass = new TypeCheckingPass(reporter);
        ast.accept(typeCheckingPass);

        if (reporter.getErrorCount() > 0) {
            return null;
        }
        SymbolTable symbolTable = typeCheckingPass.getSymbolTable();

        LabelResolutionPass labelResolutionPass = new LabelResolutionPass(reporter);
        ast.accept(labelResolutionPass);

        if (reporter.getErrorCount() > 0) {
            return null;
        }

        LoopLabelingPass loopLabelingPass = new LoopLabelingPass(reporter);
        ast.accept(loopLabelingPass);

        if (reporter.getErrorCount() > 0) {
            return null;
        }

        AstToTacLowerer astToTacLowerer = new AstToTacLowerer(symbolTable, reporter);
        TacProgram tacProgram = astToTacLowerer.lower((ProgramNode) ast);

        if (reporter.getErrorCount() > 0) {
            return null;
        }

        return new Result(symbolTable, tacProgram);
    }

    public record Result(SymbolTable symbolTable, TacProgram tacProgram) { }
}
