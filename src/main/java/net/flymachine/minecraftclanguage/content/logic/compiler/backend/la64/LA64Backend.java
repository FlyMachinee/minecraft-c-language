package net.flymachine.minecraftclanguage.content.logic.compiler.backend.la64;

import net.flymachine.minecraftclanguage.content.logic.assembler.la64.assembly.LA64AsmStatement;
import net.flymachine.minecraftclanguage.content.logic.compiler.backend.la64.highLevel.HighLevelProgram;
import net.flymachine.minecraftclanguage.content.logic.compiler.ir.TacProgram;

import java.util.List;

public final class LA64Backend {

    public LA64Backend() { }

    public List<LA64AsmStatement> compile(TacProgram program) {
        HighLevelProgram highLevelProgram = new TacToHighLevelAsmLowerer().lower(program);
        return new HighLevelAsmToAsmLowerer().lower(highLevelProgram);
    }
}
