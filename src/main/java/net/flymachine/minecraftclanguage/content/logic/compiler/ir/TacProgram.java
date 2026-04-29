package net.flymachine.minecraftclanguage.content.logic.compiler.ir;

public class TacProgram implements TacDataStructure {
    public TacFunction functionDefinition;

    public TacProgram(TacFunction functionDefinition) {
        this.functionDefinition = functionDefinition;
    }

    @Override
    public void genFormattedString(StringBuilder stringBuilder, int indentLevel, boolean indentFirstLine) {
        if (indentFirstLine) {
            stringBuilder.append("  ".repeat(indentLevel));
        }
        stringBuilder.append("TacProgram(\n");
        functionDefinition.genFormattedString(stringBuilder, indentLevel + 1, true);
        stringBuilder.append("\n").append("  ".repeat(indentLevel)).append(")");
    }
}
