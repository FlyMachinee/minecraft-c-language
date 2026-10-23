package net.flymachine.minecraftclanguage.content.logic.compiler.optimizer.util;

import net.flymachine.minecraftclanguage.content.logic.compiler.common.type.PointerType;
import net.flymachine.minecraftclanguage.content.logic.compiler.common.type.Type;
import net.flymachine.minecraftclanguage.content.logic.compiler.frontend.c99.SymbolTable;
import net.flymachine.minecraftclanguage.content.logic.compiler.ir.inst.*;
import net.flymachine.minecraftclanguage.content.logic.compiler.ir.operand.TacConstant;
import net.flymachine.minecraftclanguage.content.logic.compiler.ir.operand.TacValue;
import net.flymachine.minecraftclanguage.content.logic.compiler.ir.operand.TacVariable;

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

    public static Type getType(SymbolTable symbolTable, TacValue val) {
        if (val instanceof TacConstant tacConstant) {
            return tacConstant.value.getType();
        } else if (val instanceof TacVariable tacVariable) {
            SymbolTable.Entry entry = symbolTable.get(tacVariable.name);
            if (entry.attr instanceof SymbolTable.Entry.AutoAttr autoAttr && autoAttr.passByReference) {
                // 通过引用传递的大结构体/联合体
                assert entry.type.isRecord() && entry.type.sizeof() > 16;
                return new PointerType(entry.type);
            }
            return entry.type;
        }
        throw new IllegalArgumentException("Unsupported TacValue type: " + val.getClass().getName());
    }
}
