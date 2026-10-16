package net.flymachine.minecraftclanguage.content.logic.compiler;

import net.flymachine.minecraftclanguage.content.logic.assembler.la64.assembly.LA64AsmStatement;
import net.flymachine.minecraftclanguage.content.logic.assembler.la64.assembly.LA64Assembly;
import net.flymachine.minecraftclanguage.content.logic.compiler.backend.la64.LA64Backend;
import net.flymachine.minecraftclanguage.content.logic.compiler.frontend.c99.C99Frontend;
import net.flymachine.minecraftclanguage.content.logic.compiler.optimizer.TacOptimizer;
import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;

import javax.annotation.Nullable;
import java.util.List;

public final class C99ToLA64Compiler {

    private final CompilerOptions options;

    public C99ToLA64Compiler() {
        this.options = CompilerOptions.DEFAULT;
    }

    public C99ToLA64Compiler(CompilerOptions options) {
        this.options = options;
    }

    public @Nullable LA64Assembly compile(String source) {
        return compile(CharStreams.fromString(source));
    }

    public @Nullable LA64Assembly compile(String source, CompilerOptions options) {
        return compile(CharStreams.fromString(source), options);
    }

    public @Nullable LA64Assembly compile(String source, String fileName) {
        return compile(CharStreams.fromString(source, fileName));
    }

    public @Nullable LA64Assembly compile(String source, String fileName, CompilerOptions options) {
        return compile(CharStreams.fromString(source, fileName), options);
    }

    public @Nullable LA64Assembly compile(CharStream charStream) {
        return compile(charStream, options);
    }

    /**
     * 将指定的字符流编译成 LA64 汇编指令列表
     *
     * @param charStream 包含源文件内容的字符流
     * @param options    单次编译选项，仅本次起效
     * @return 编译后的 LA64 汇编指令列表，如果遇到错误则返回 {@code null}
     */
    public @Nullable LA64Assembly compile(CharStream charStream, CompilerOptions options) {
        C99Frontend.Result frontendResult = new C99Frontend().compile(charStream, options);
        if (frontendResult == null) {
            return null;
        }

        if (options.optimization().level() != CompilerOptions.OptimizationLevel.O0) {
            TacOptimizer optimizer = new TacOptimizer();
            optimizer.optimize(frontendResult.tacProgram(), frontendResult.symbolTable(), options);
        }

        String newName = charStream.getSourceName().replaceAll("\\.[^.]+$", "") + ".s";
        List<LA64AsmStatement> asm = new LA64Backend().compile(frontendResult, options);
        return new LA64Assembly(asm, newName);
    }

}
