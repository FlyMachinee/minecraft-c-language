package net.flymachine.minecraftclanguage.content.logic.compiler.optimizer.cfg;

import net.flymachine.minecraftclanguage.content.logic.compiler.ir.inst.TacInstruction;
import net.flymachine.minecraftclanguage.content.logic.compiler.optimizer.cfg.node.BasicBlock;
import net.flymachine.minecraftclanguage.content.logic.compiler.optimizer.cfg.node.GraphNode;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
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

    /**
     * 生成只含节点名称的概览图 DOT 文本
     */
    public static String toDotOverview(ControlFlowGraph cfg, String graphName) {
        StringBuilder dot = new StringBuilder(512);
        appendDotHeader(dot, graphName + "_overview");
        appendDotNodes(dot, cfg, false);
        appendDotEdges(dot, cfg);
        dot.append("}\n");
        return dot.toString();
    }

    /**
     * 生成带指令序列的完整图 DOT 文本
     */
    public static String toDotDetailed(ControlFlowGraph cfg, String graphName) {
        StringBuilder dot = new StringBuilder(1024);
        appendDotHeader(dot, graphName + "_detailed");
        appendDotNodes(dot, cfg, true);
        appendDotEdges(dot, cfg);
        dot.append("}\n");
        return dot.toString();
    }

    private static void appendDotHeader(StringBuilder dot, String graphName) {
        dot.append("digraph \"").append(graphName).append("\" {\n");
        dot.append("    graph [bgcolor=\"transparent\", splines=true, rankdir=TB, dpi=300,"
                   + " fontname=\"Fira Code\", fontcolor=\"black\"];\n");
        dot.append("    node [shape=box, style=rounded, fontname=\"Fira Code\","
                   + " fontcolor=\"black\", color=\"black\"];\n");
        dot.append("    edge [color=\"black\", dir=forward, fontname=\"Fira Code\","
                   + " fontcolor=\"black\"];\n");
    }

    private static void appendDotNodes(StringBuilder dot, ControlFlowGraph cfg, boolean detailed) {
        dot.append("    ").append(dotId(cfg.entry())).append(" [label=<<B>Entry</B>>];\n");
        dot.append("    ").append(dotId(cfg.exit())).append(" [label=<<B>Exit</B>>];\n");

        for (BasicBlock block : cfg.blocks()) {
            dot.append("    ").append(dotId(block));
            if (detailed) {
                dot.append(" [xlabel=").append(blockTitle(block))
                   .append(", label=").append(detailedLabel(block)).append("]");
            } else {
                dot.append(" [label=").append(blockTitle(block)).append("]");
            }
            dot.append(";\n");
        }
    }

    private static void appendDotEdges(StringBuilder dot, ControlFlowGraph cfg) {
        for (GraphNode node : cfg.nodes()) {
            for (int successorId : node.successors()) {
                GraphNode successor = cfg.node(successorId);
                if (successor == null) {
                    continue;
                }
                dot.append("    ").append(dotId(node))
                   .append(" -> ").append(dotId(successor)).append(";\n");
            }
        }
    }

    private static String dotId(GraphNode node) {
        if (node.isEntry()) {
            return "Entry";
        }
        if (node.isExit()) {
            return "Exit";
        }
        return "B" + node.id();
    }

    private static String blockTitle(BasicBlock block) {
        return "<<B>B</B><SUB>" + block.id() + "</SUB>>";
    }

    private static String detailedLabel(BasicBlock block) {
        StringBuilder html = new StringBuilder();
        html.append("<<TABLE BORDER=\"0\" CELLBORDER=\"0\" CELLSPACING=\"0\" CELLPADDING=\"2\">");

        if (block.labels().isEmpty() && block.instructions().isEmpty()) {
            html.append("<TR><TD ALIGN=\"LEFT\">&lt;empty&gt;</TD></TR>");
        } else {
            for (String label : block.labels()) {
                html.append("<TR><TD ALIGN=\"LEFT\"><I>")
                    .append(escapeHtml(label))
                    .append(":</I></TD></TR>");
            }
            for (TacInstruction instruction : block.instructions()) {
                html.append("<TR><TD ALIGN=\"LEFT\">")
                    .append(escapeHtml(prettyOf(instruction)))
                    .append("</TD></TR>");
            }
        }

        html.append("</TABLE>>");
        return html.toString();
    }

    private static String prettyOf(TacInstruction instruction) {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        PrintStream stream = new PrintStream(buffer, true, StandardCharsets.UTF_8);
        instruction.dumpPretty(stream, 0, false);
        stream.flush();
        return buffer.toString(StandardCharsets.UTF_8);
    }

    private static String escapeHtml(String raw) {
        return raw.replace("\r", "")
                  .replace("\n", " ")
                  .replace("&", "&amp;")
                  .replace("<", "&lt;")
                  .replace(">", "&gt;");
    }
}
