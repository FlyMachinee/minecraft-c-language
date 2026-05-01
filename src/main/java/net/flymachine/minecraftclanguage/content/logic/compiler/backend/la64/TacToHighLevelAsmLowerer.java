package net.flymachine.minecraftclanguage.content.logic.compiler.backend.la64;

import net.flymachine.minecraftclanguage.content.logic.compiler.backend.la64.highLevel.HighLevelFunction;
import net.flymachine.minecraftclanguage.content.logic.compiler.backend.la64.highLevel.HighLevelProgram;
import net.flymachine.minecraftclanguage.content.logic.compiler.ir.TacFunction;
import net.flymachine.minecraftclanguage.content.logic.compiler.ir.TacInstruction;
import net.flymachine.minecraftclanguage.content.logic.compiler.ir.TacProgram;

import java.util.List;

public final class TacToHighLevelAsmLowerer {

    public TacToHighLevelAsmLowerer() { }

    public HighLevelProgram lower(TacProgram tacProgram) {
        HighLevelFunction highLevelFunction = lowerFunction(tacProgram.functionDefinition);
        return new HighLevelProgram(highLevelFunction);
    }

    private HighLevelFunction lowerFunction(TacFunction tacFunction) {
        List<TacInstruction> instructions = tacFunction.instructions;
        LA64TacVisitor visitor = new LA64TacVisitor();
        for (TacInstruction instruction : instructions) {
            instruction.accept(visitor);
        }
        return new HighLevelFunction(tacFunction.name, visitor.getTarget());
    }
}
