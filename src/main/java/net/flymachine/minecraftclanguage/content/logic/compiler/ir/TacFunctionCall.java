package net.flymachine.minecraftclanguage.content.logic.compiler.ir;

import java.util.List;

public class TacFunctionCall implements TacInstruction {
    public String funcName;
    public List<TacValue> args;
    public TacValue dst;

    public TacFunctionCall(String funcName, List<TacValue> args, TacValue dst) {
        this.funcName = funcName;
        this.args = args;
        this.dst = dst;
    }

    @Override
    public <T> T accept(TacVisitor<T> visitor) {
        return visitor.visit(this);
    }

    @Override
    public void genFormattedString(StringBuilder stringBuilder, int indentLevel, boolean indentFirstLine) {
        if (indentFirstLine) {
            stringBuilder.append("  ".repeat(indentLevel));
        }
        stringBuilder.append("FunctionCall(name=").append(this.funcName).append(", args=[");
        for (int i = 0; i < this.args.size(); i++) {
            if (i > 0) {
                stringBuilder.append(", ");
            }
            TacValue arg = this.args.get(i);
            arg.genFormattedString(stringBuilder);
        }
        stringBuilder.append("], dst=");
        dst.genFormattedString(stringBuilder);
        stringBuilder.append(")");
    }
}
