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

public final class C99Frontend {

    public C99Frontend() { }

    public TacProgram compile(CharStream charStream) {
        C99Lexer lexer = new C99Lexer(charStream);
        C99Parser parser = new C99Parser(new CommonTokenStream(lexer));
        C99Parser.CompilationUnitContext tree = parser.compilationUnit();
        AstNode ast = new AstBuilderVisitor().visit(tree);
        return new AstToTacLowerer().lower((ProgramNode) ast);
    }
}
