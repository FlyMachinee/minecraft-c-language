package net.flymachine.minecraftclanguage.content.logic.compiler.backend.la64.highLevel.instruction;

import net.flymachine.minecraftclanguage.content.logic.compiler.backend.la64.highLevel.AsmType;
import net.flymachine.minecraftclanguage.content.logic.compiler.backend.la64.highLevel.operand.HighLevelOperand;

import java.io.PrintStream;

/**
 * 如有需要，会将 cond 加载至 T0
 * <p>
 * 可能使用的临时寄存器：T0
 */
public class BranchIfNotZero implements HighLevelInstruction {
    public final AsmType asmType;
    /**
     * 如有需要，会将 cond 加载至 T0
     */
    public HighLevelOperand cond;
    public final String target;

    public BranchIfNotZero(AsmType asmType, HighLevelOperand cond, String target) {
        if (asmType == AsmType.DOUBLE) {
            throw new IllegalArgumentException("BranchIfNotZero does not support DOUBLE type");
        }
        this.asmType = asmType;
        this.cond = cond;
        this.target = target;
    }

    @Override
    public void dumpPretty(PrintStream stream, int indentLevel, boolean indentFirstLine) {
        if (indentFirstLine) {
            indent(stream, indentLevel);
        }
        stream.print("if ");
        cond.dumpPretty(stream);
        stream.print(" != 0 goto ");
        stream.print(target);
        stream.print(" [");
        stream.print(asmType);
        stream.print("]");
    }

    @Override
    public <T> T accept(HighLevelVisitor<T> visitor) {
        return visitor.visit(this);
    }
}
