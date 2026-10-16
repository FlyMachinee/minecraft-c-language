package net.flymachine.minecraftclanguage.content.logic.compiler.ir.inst;

import net.flymachine.minecraftclanguage.content.logic.compiler.ir.TacDataStructure;
import net.flymachine.minecraftclanguage.content.logic.compiler.ir.TacVisitor;

public interface TacInstruction extends TacDataStructure {
    <T> T accept(TacVisitor<T> visitor);
}
