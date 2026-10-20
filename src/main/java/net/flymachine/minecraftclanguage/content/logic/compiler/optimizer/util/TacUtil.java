package net.flymachine.minecraftclanguage.content.logic.compiler.optimizer.util;

import net.flymachine.minecraftclanguage.content.logic.compiler.ir.inst.*;

import javax.annotation.Nullable;

public final class TacUtil {

    private TacUtil() { }

    public static @Nullable String getLabel(TacInstruction instruction) {
        if (instruction instanceof TacJump jump) {
            return jump.target;
        } else if (instruction instanceof TacJumpIfZero jump) {
            return jump.target;
        } else if (instruction instanceof TacJumpIfNotZero jump) {
            return jump.target;
        } else if (instruction instanceof TacJumpIfComparison jump) {
            return jump.target;
        }
        return null;
    }
}
