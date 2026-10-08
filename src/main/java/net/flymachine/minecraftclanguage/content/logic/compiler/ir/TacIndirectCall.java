package net.flymachine.minecraftclanguage.content.logic.compiler.ir;

import org.jetbrains.annotations.Nullable;

import java.io.PrintStream;
import java.util.List;

public class TacIndirectCall implements TacInstruction {
    public TacValue funcPtr;
    public List<TacValue> args;
    public @Nullable TacValue dst;

    public TacIndirectCall(TacValue funcPtr, List<TacValue> args, @Nullable TacValue dst) {
        this.funcPtr = funcPtr;
        this.args = args;
        this.dst = dst;
    }

    public TacIndirectCall(TacValue funcPtr, List<TacValue> args) {
        this.funcPtr = funcPtr;
        this.args = args;
        this.dst = null;
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
        stream.append("IndirectCall(pointer=");
        funcPtr.dump(stream);
        stream.append(", args=[");
        for (int i = 0; i < this.args.size(); i++) {
            if (i > 0) {
                stream.print(", ");
            }
            TacValue arg = this.args.get(i);
            arg.dump(stream);
        }
        stream.print("], dst=");
        if (dst != null) {
            dst.dump(stream);
        } else {
            stream.print("null");
        }
        stream.print(")");
    }

    @Override
    public void dumpPretty(PrintStream stream, int indentLevel, boolean indentFirstLine) {
        if (indentFirstLine) {
            stream.print("  ".repeat(indentLevel));
        }
        // dst = funcPtr(args)
        if (dst != null) {
            dst.dumpPretty(stream);
            stream.print(" = ");
        }
        funcPtr.dumpPretty(stream);
        stream.print("(");
        for (int i = 0; i < this.args.size(); i++) {
            if (i > 0) {
                stream.print(", ");
            }
            TacValue arg = this.args.get(i);
            arg.dumpPretty(stream);
        }
        stream.print(")");
    }
}
