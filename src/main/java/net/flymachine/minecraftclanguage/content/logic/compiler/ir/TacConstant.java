package net.flymachine.minecraftclanguage.content.logic.compiler.ir;

import net.flymachine.minecraftclanguage.content.logic.compiler.common.constant.Constant;

public class TacConstant implements TacValue {
    public Constant value;

    public TacConstant(Constant value) {
        this.value = value;
    }

    @Override
    public void genFormattedString(StringBuilder stringBuilder, int indentLevel, boolean indentFirstLine) {
        stringBuilder.append("Constant(").append(value).append(")");
    }
}
