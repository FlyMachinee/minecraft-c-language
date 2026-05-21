package net.flymachine.minecraftclanguage.content.logic.compiler;

import net.flymachine.minecraftclanguage.content.logic.assembler.la64.assembly.LA64AsmStatement;
import net.flymachine.minecraftclanguage.content.logic.assembler.la64.assembly.LA64Assembly;
import net.flymachine.minecraftclanguage.content.logic.compiler.backend.la64.LA64Backend;
import net.flymachine.minecraftclanguage.content.logic.compiler.frontend.c99.C99Frontend;
import net.flymachine.minecraftclanguage.content.logic.compiler.ir.TacProgram;
import net.flymachine.minecraftclanguage.content.logic.compiler.optimizer.TacOptimizer;
import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;

import javax.annotation.Nullable;
import java.util.List;

public final class C99ToLA64Compiler {

    public @Nullable LA64Assembly compile(String source) {
        return compile(CharStreams.fromString(source));
    }

    public @Nullable LA64Assembly compile(String source, String fileName) {
        return compile(CharStreams.fromString(source, fileName));
    }

    /**
     * 将指定的字符流编译成 LA64 汇编指令列表
     *
     * @param charStream 包含源文件内容的字符流
     * @return 编译后的 LA64 汇编指令列表，如果遇到错误则返回 {@code null}
     */
    public @Nullable LA64Assembly compile(CharStream charStream) {
        C99Frontend.Result frontendResult = new C99Frontend().compile(charStream);
        if (frontendResult == null) {
            return null;
        }
        TacProgram optimizedTacProgram = new TacOptimizer().optimize(frontendResult.tacProgram());
        String newName = charStream.getSourceName().replaceAll("\\.[^.]+$", "") + ".s";
        List<LA64AsmStatement> asm = new LA64Backend().compile(
            new C99Frontend.Result(frontendResult.symbolTable(), optimizedTacProgram));
        return new LA64Assembly(asm, newName);
    }

}
