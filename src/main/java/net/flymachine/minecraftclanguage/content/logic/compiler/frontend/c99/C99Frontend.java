package net.flymachine.minecraftclanguage.content.logic.compiler.frontend.c99;

import net.flymachine.minecraftclanguage.content.logic.compiler.frontend.antlr.C99Lexer;
import net.flymachine.minecraftclanguage.content.logic.compiler.frontend.antlr.C99Parser;
import net.flymachine.minecraftclanguage.content.logic.compiler.frontend.c99.ast.node.AstNode;
import net.flymachine.minecraftclanguage.content.logic.compiler.frontend.c99.ast.node.ProgramNode;
import net.flymachine.minecraftclanguage.content.logic.compiler.ir.TacProgram;
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
     * @return 前端编译后的中间表示，如果遇到错误则返回 null
     */
    public @Nullable TacProgram compile(CharStream charStream) {
        SourceFile sourceFile = new SourceFile(charStream);

        C99Lexer lexer = new C99Lexer(charStream);
        lexer.removeErrorListeners();
        LexerErrorListener lexerErrorListener = new LexerErrorListener();
        lexerErrorListener.setSourceFile(sourceFile);
        lexer.addErrorListener(lexerErrorListener);

        C99Parser parser = new C99Parser(new CommonTokenStream(lexer));
        parser.removeErrorListeners();
        ParserErrorListener parserErrorListener = new ParserErrorListener();
        parserErrorListener.setSourceFile(sourceFile);
        parser.addErrorListener(parserErrorListener);

        C99Parser.CompilationUnitContext tree = parser.compilationUnit();
        if (lexerErrorListener.hasErrors() || parserErrorListener.hasErrors()) {
            return null;
        }

        AstNode ast = new AstBuilderVisitor().visit(tree);

        IdentifierResolutionPass identifierResolutionPass = new IdentifierResolutionPass();
        identifierResolutionPass.setSourceFile(sourceFile);
        ast.accept(identifierResolutionPass);

        if (identifierResolutionPass.hasSemanticError()) {
            return null;
        }

        TypeCheckingPass typeCheckingPass = new TypeCheckingPass();
        typeCheckingPass.setSourceFile(sourceFile);
        ast.accept(typeCheckingPass);

        if (typeCheckingPass.hasSemanticError()) {
            return null;
        }

        LabelResolutionPass labelResolutionPass = new LabelResolutionPass();
        labelResolutionPass.setSourceFile(sourceFile);
        ast.accept(labelResolutionPass);

        if (labelResolutionPass.hasSemanticError()) {
            return null;
        }

        LoopLabelingPass loopLabelingPass = new LoopLabelingPass();
        loopLabelingPass.setSourceFile(sourceFile);
        ast.accept(loopLabelingPass);

        if (loopLabelingPass.hasSemanticError()) {
            return null;
        }

        return new AstToTacLowerer().lower((ProgramNode) ast);
    }
}
