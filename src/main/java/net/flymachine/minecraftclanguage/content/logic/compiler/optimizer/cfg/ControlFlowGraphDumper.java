package net.flymachine.minecraftclanguage.content.logic.compiler.optimizer.cfg;

import net.flymachine.minecraftclanguage.content.logic.compiler.ir.inst.TacInstruction;
import net.flymachine.minecraftclanguage.content.logic.compiler.optimizer.cfg.node.BasicBlock;
import net.flymachine.minecraftclanguage.content.logic.compiler.optimizer.cfg.node.GraphNode;

import java.io.PrintStream;
import java.util.List;

public final class ControlFlowGraphDumper {

    private ControlFlowGraphDumper() { }

    public static void dump(ControlFlowGraph cfg, PrintStream stream) {
        stream.println("ControlFlowGraph(");
        for (GraphNode node : cfg.nodes()) {
            dumpNode(node, stream, 1);
            stream.println(",");
        }
        indent(stream, 0);
        stream.append(")");
    }

    private static void dumpNode(GraphNode node, PrintStream stream, int indentLevel) {
        switch (node.type()) {
            case ENTRY -> {
                indent(stream, indentLevel);
                stream.println("EntryNode(");
                field(stream, indentLevel + 1, "id", String.valueOf(node.id()));
                field(stream, indentLevel + 1, "successors", String.valueOf(node.successors()));
                indent(stream, indentLevel);
                stream.append(")");
            }
            case EXIT -> {
                indent(stream, indentLevel);
                stream.println("ExitNode(");
                field(stream, indentLevel + 1, "id", String.valueOf(node.id()));
                field(stream, indentLevel + 1, "predecessors", String.valueOf(node.predecessors()));
                indent(stream, indentLevel);
                stream.append(")");
            }
            case BASIC_BLOCK -> {
                BasicBlock block = (BasicBlock) node;
                indent(stream, indentLevel);
                stream.println("BasicBlock(");
                field(stream, indentLevel + 1, "id", String.valueOf(block.id()));
                field(stream, indentLevel + 1, "labels", String.valueOf(block.labels()));
                field(stream, indentLevel + 1, "predecessors", String.valueOf(block.predecessors()));
                field(stream, indentLevel + 1, "successors", String.valueOf(block.successors()));
                indent(stream, indentLevel + 1);
                stream.append("instructions=[\n");
                for (TacInstruction instruction : block.instructions()) {
                    instruction.dump(stream, indentLevel + 2, true);
                    stream.println(",");
                }
                indent(stream, indentLevel + 1);
                stream.append("]\n");
                indent(stream, indentLevel);
                stream.append(")");
            }
            default -> throw new IllegalStateException("Unknown node type: " + node.type());
        }
    }

    private static void field(PrintStream stream, int indentLevel, String name, String value) {
        indent(stream, indentLevel);
        stream.append(name).append('=').append(value).append(",\n");
    }

    public static void dumpPretty(ControlFlowGraph cfg, PrintStream stream) {
        stream.append("ControlFlowGraph(")
              .append(String.valueOf(cfg.nodes().size()))
              .append(" nodes) {\n");

        indent(stream, 1);
        stream.println("[ENTRY]");

        for (GraphNode node : cfg.nodes()) {
            if (node.isEntry() || node.isExit()) {
                continue;
            }
            dumpPrettyBlock((BasicBlock) node, stream, 1);
        }

        indent(stream, 1);
        stream.println("[EXIT]");

        indent(stream, 0);
        stream.append('}');
    }

    private static void dumpPrettyBlock(BasicBlock block, PrintStream stream, int indentLevel) {
        indent(stream, indentLevel);
        stream.append('[').append(String.valueOf(block.id())).append(']');
        if (!block.labels().isEmpty()) {
            stream.append(' ');
            List<String> labels = block.labels();
            if (labels.size() == 1) {
                stream.append(labels.get(0));
            } else {
                stream.append(String.valueOf(labels));
            }
            stream.append(":");
        }
        stream.append(" pred=").append(String.valueOf(block.predecessors()));
        stream.append(" succ=").append(String.valueOf(block.successors()));
        stream.println();

        for (TacInstruction instruction : block.instructions()) {
            instruction.dumpPretty(stream, indentLevel + 1, true);
            stream.println();
        }
    }

    private static void indent(PrintStream stream, int indentLevel) {
        stream.print("    ".repeat(indentLevel));
    }
}
