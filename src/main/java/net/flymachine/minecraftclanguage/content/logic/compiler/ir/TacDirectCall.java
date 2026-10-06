package net.flymachine.minecraftclanguage.content.logic.compiler.ir;

import java.io.PrintStream;
import java.util.List;

public class TacDirectCall implements TacInstruction {
    public String funcDesignator;
    public List<TacValue> args;
    public TacValue dst;

    public TacDirectCall(String funcDesignator, List<TacValue> args, TacValue dst) {
        this.funcDesignator = funcDesignator;
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
        stream.append("DirectCall(designator=").append(this.funcDesignator).append(", args=[");
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

    @Override
    public void dumpPretty(PrintStream stream, int indentLevel, boolean indentFirstLine) {
        if (indentFirstLine) {
            stream.print("  ".repeat(indentLevel));
        }
        // dst = funcDesignator(args)
        dst.dumpPretty(stream);
        stream.print(" = &");
        stream.print(funcDesignator);
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
