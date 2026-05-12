package net.flymachine.minecraftclanguage.content.logic.compiler.backend.la64.highLevel;

import java.util.List;

public class HighLevelProgram {
    public List<HighLevelFunction> functionDefinitions;

    public HighLevelProgram(List<HighLevelFunction> functionDefinitions) {
        this.functionDefinitions = functionDefinitions;
    }
}
