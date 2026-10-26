package net.flymachine.minecraftclanguage.content.logic.compiler.backend.la64;

import net.flymachine.minecraftclanguage.content.logic.assembler.la64.assembly.LA64AsmStatement;
import net.flymachine.minecraftclanguage.content.logic.compiler.CompilerOptions;
import net.flymachine.minecraftclanguage.content.logic.compiler.backend.la64.highLevel.HighLevelFunction;
import net.flymachine.minecraftclanguage.content.logic.compiler.backend.la64.highLevel.HighLevelProgram;
import net.flymachine.minecraftclanguage.content.logic.compiler.backend.la64.highLevel.HighLevelTopLevel;
import net.flymachine.minecraftclanguage.content.logic.compiler.backend.la64.registerAllocator.linear.SimpleLinearAllocator;
import net.flymachine.minecraftclanguage.content.logic.compiler.frontend.c99.C99Frontend;

import java.util.List;

public final class LA64Backend {

    public LA64Backend() { }

    public List<LA64AsmStatement> compile(C99Frontend.Result frontendResult, CompilerOptions options) {
        TacToHighLevelAsmLowerer tacToHLAsm = new TacToHighLevelAsmLowerer(frontendResult.symbolTable());
        HighLevelProgram highLevelProgram = tacToHLAsm.lower(frontendResult.tacProgram());
        BackendSymbolTable backendSymbolTable = tacToHLAsm.getBackendSymbolTable();

        if (options.dumping().contains(CompilerOptions.DumpKind.HL_ASM_RAW)) {
            System.out.println("=== Dumping High-Level Assembly before pseudo operand replacement ===");
            highLevelProgram.dumpPretty(System.out);
            System.out.println("=== Dumping Complete ===");
            System.out.println();
            if (options.dumping().contains(CompilerOptions.DumpKind.HL_ASM_RAW_SYMTAB)) {
                System.out.println("=== Dumping Backend Symbol Table ===");
                backendSymbolTable.dump(System.out);
                System.out.println("=== Dumping Complete ===");
                System.out.println();
            }
        }

        if (options.optimization().level() != CompilerOptions.OptimizationLevel.O0) {
            SimpleLinearAllocator allocator = new SimpleLinearAllocator(backendSymbolTable);
            for (HighLevelTopLevel topLevel : highLevelProgram.topLevels) {
                if (topLevel instanceof HighLevelFunction func) {
                    allocator.allocate(func);
                }
            }

            if (options.dumping().contains(CompilerOptions.DumpKind.HL_ASM_REG_ALLOC)) {
                System.out.println("=== Dumping High-Level Assembly after register allocation ===");
                highLevelProgram.dumpPretty(System.out);
                System.out.println("=== Dumping Complete ===");
                System.out.println();
            }
        }

        ReplacePseudoOperandPass replacePseudoOperandPass = new ReplacePseudoOperandPass(backendSymbolTable);
        replacePseudoOperandPass.runOnProgram(highLevelProgram);

        if (options.dumping().contains(CompilerOptions.DumpKind.HL_ASM_PSEUDO)) {
            System.out.println("=== Dumping High-Level Assembly after pseudo operand replacement ===");
            highLevelProgram.dumpPretty(System.out);
            System.out.println("=== Dumping Complete ===");
            System.out.println();
        }

        return new HighLevelAsmToAsmLowerer(backendSymbolTable).lower(highLevelProgram);
    }
}
