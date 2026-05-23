package net.flymachine.minecraftclanguage.content.logic.compiler.ir;

import java.io.PrintStream;
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
    public void dump(PrintStream stream, int indentLevel, boolean indentFirstLine) {
        if (indentFirstLine) {
            stream.print("  ".repeat(indentLevel));
        }
        stream.append("FunctionCall(name=").append(this.funcName).append(", args=[");
        for (int i = 0; i < this.args.size(); i++) {
            if (i > 0) {
                stream.print(", ");
            }
            TacValue arg = this.args.get(i);
            arg.dump(stream);
        }
        stream.print("], dst=");
        dst.dump(stream);
        stream.print(")");
    }
}
