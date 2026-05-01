package net.flymachine.minecraftclanguage.content.logic.compiler.ir;

public interface TacInstruction extends TacDataStructure {
    <T> T accept(TacVisitor<T> visitor);
}
