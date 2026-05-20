package net.flymachine.minecraftclanguage.content.logic.compiler.backend.la64;

import net.flymachine.minecraftclanguage.content.logic.assembler.la64.assembly.LA64AsmStatement;
import net.flymachine.minecraftclanguage.content.logic.compiler.backend.la64.highLevel.HighLevelProgram;
import net.flymachine.minecraftclanguage.content.logic.compiler.frontend.c99.C99Frontend;

import java.util.List;

public final class LA64Backend {

    public LA64Backend() { }

    public List<LA64AsmStatement> compile(C99Frontend.Result frontendResult) {
        TacToHighLevelAsmLowerer tacToHLAsm = new TacToHighLevelAsmLowerer(frontendResult.symbolTable());
        HighLevelProgram highLevelProgram = tacToHLAsm.lower(frontendResult.tacProgram());
        BackendSymbolTable backendSymbolTable = tacToHLAsm.getBackendSymbolTable();
        ReplacePseudoRegisterPass replacePseudoRegisterPass =
            new ReplacePseudoRegisterPass(backendSymbolTable);
        replacePseudoRegisterPass.runOnProgram(highLevelProgram);
        return new HighLevelAsmToAsmLowerer(backendSymbolTable).lower(highLevelProgram);
    }
}
