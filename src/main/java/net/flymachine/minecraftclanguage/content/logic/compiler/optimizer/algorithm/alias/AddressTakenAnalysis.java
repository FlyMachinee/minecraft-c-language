package net.flymachine.minecraftclanguage.content.logic.compiler.optimizer.algorithm.alias;

import net.flymachine.minecraftclanguage.content.logic.compiler.ir.inst.TacGetAddress;
import net.flymachine.minecraftclanguage.content.logic.compiler.ir.operand.TacVariable;
import net.flymachine.minecraftclanguage.content.logic.compiler.optimizer.algorithm.common.FunctionContext;

import java.util.BitSet;

public final class AddressTakenAnalysis {

    private AddressTakenAnalysis() {
    }

    public static BitSet analyze(FunctionContext ctx) {
        return new AddressTakenAnalysis().run(ctx);
    }

    private BitSet run(FunctionContext ctx) {
        BitSet aliasedVars = new BitSet();
        for (var block : ctx.cfg().blocks()) {
            for (var inst : block.instructions()) {
                if (inst instanceof TacGetAddress getAddress) {
                    TacVariable variable = (TacVariable) getAddress.src;
                    aliasedVars.set(ctx.interner().intern(variable.name));
                }
            }
        }
        return aliasedVars;
    }

}
