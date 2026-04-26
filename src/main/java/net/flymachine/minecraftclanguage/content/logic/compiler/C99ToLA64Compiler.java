package net.flymachine.minecraftclanguage.content.logic.compiler;

import net.flymachine.minecraftclanguage.content.logic.assembler.la64.assembly.LA64AsmStatement;
import net.flymachine.minecraftclanguage.content.logic.compiler.backend.la64.LA64Backend;
import net.flymachine.minecraftclanguage.content.logic.compiler.backend.machineIndependent.TacOptimizer;
import net.flymachine.minecraftclanguage.content.logic.compiler.frontend.c99.C99Frontend;
import net.flymachine.minecraftclanguage.content.logic.compiler.ir.TacProgram;
import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;

import java.util.List;

public final class C99ToLA64Compiler {

    public C99ToLA64Compiler() { }

    public List<LA64AsmStatement> compile(String source) {
        return compile(CharStreams.fromString(source));
    }

    public List<LA64AsmStatement> compile(CharStream charStream) {
        TacProgram tacProgram = new C99Frontend().compile(charStream);
        TacProgram optimizedTacProgram = new TacOptimizer().optimize(tacProgram);
        return new LA64Backend().compile(optimizedTacProgram);
    }
}
