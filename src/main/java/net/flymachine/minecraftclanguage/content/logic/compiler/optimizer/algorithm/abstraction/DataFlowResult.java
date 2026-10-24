package net.flymachine.minecraftclanguage.content.logic.compiler.optimizer.algorithm.abstraction;

import java.util.BitSet;

public record DataFlowResult(BitSet[] befores, BitSet[] afters) { }
