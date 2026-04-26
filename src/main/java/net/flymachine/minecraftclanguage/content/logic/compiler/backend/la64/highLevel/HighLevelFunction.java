package net.flymachine.minecraftclanguage.content.logic.compiler.backend.la64.highLevel;

import net.flymachine.minecraftclanguage.content.logic.compiler.backend.la64.highLevel.instruction.HighLevelInstruction;

import java.util.List;

public class HighLevelFunction {
    public String name;
    public List<HighLevelInstruction> instructions;

    public HighLevelFunction(String name, List<HighLevelInstruction> instructions) {
        this.name = name;
        this.instructions = instructions;
    }
}

