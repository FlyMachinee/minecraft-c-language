package net.flymachine.minecraftclanguage.content.logic.compiler.frontend.c99;

import net.flymachine.minecraftclanguage.content.logic.compiler.common.staticInit.StaticInit;
import net.flymachine.minecraftclanguage.content.logic.compiler.common.type.Type;
import net.flymachine.minecraftclanguage.content.logic.compiler.frontend.c99.ast.node.IdentifierNode;
import net.flymachine.minecraftclanguage.content.logic.compiler.frontend.c99.ast.node.type.TypeNode;

import java.io.PrintStream;
import java.util.*;

public final class SymbolTable {

    public SymbolTable() { }

    public Entry get(String identifier) {
        return symbolTable.get(identifier);
    }

    public void put(String identifier, Entry symbolEntry) {
        symbolTable.put(identifier, symbolEntry);
    }

    public Entry remove(String identifier) {
        return symbolTable.remove(identifier);
    }

    public boolean contains(String identifier) {
        return symbolTable.containsKey(identifier);
    }

    public void clear() {
        symbolTable.clear();
    }

    public Collection<Entry> getEntries() {
        return symbolTable.values();
    }

    private final Map<String, Entry> symbolTable = new HashMap<>();

    public static final class Entry {
        public IdentifierNode id;
        TypeNode typeNode; // 仅用于信息打印
        public Type type;
        public IdentifierAttr attr;

        public Entry(IdentifierNode id, TypeNode typeNode, Type type, IdentifierAttr attr) {
            this.id = id;
            this.typeNode = typeNode;
            this.type = type;
            this.attr = attr;
        }

        public sealed interface IdentifierAttr permits AutoAttr, ConstantAttr, EnumeratorAttr, FuncAttr, StaticAttr {
            boolean isDefinition();

            boolean isGlobal();
        }

        public static final class FuncAttr implements IdentifierAttr {
            public boolean defined;
            /**
             * 是否对其他编译单元可见
             */
            public boolean global;

            public FuncAttr(boolean defined, boolean global) {
                this.defined = defined;
                this.global = global;
            }

            @Override
            public boolean isDefinition() {
                return defined;
            }

            @Override
            public boolean isGlobal() {
                return global;
            }
        }

        public static final class StaticAttr implements IdentifierAttr {
            public DefinitionType defType;
            /**
             * 是否对其他编译单元可见
             */
            public boolean global;

            public StaticAttr(DefinitionType defType, boolean global) {
                this.defType = defType;
                this.global = global;
            }

            @Override
            public boolean isDefinition() {
                return defType instanceof Defined;
            }

            @Override
            public boolean isGlobal() {
                return global;
            }

            public sealed interface DefinitionType permits Defined, NoDefinition, Tentative { }

            public static final class Tentative implements DefinitionType {
                private Tentative() { }

                public static final Tentative INSTANCE = new Tentative();
            }

            public static final class NoDefinition implements DefinitionType {
                private NoDefinition() { }

                public static final NoDefinition INSTANCE = new NoDefinition();
            }

            public record Defined(List<StaticInit> init) implements DefinitionType { }
        }

        public static final class AutoAttr implements IdentifierAttr {
            public boolean passByReference = false; // 仅作用于大结构体作为函数参数时

            public AutoAttr() { }

            @Override
            public boolean isDefinition() {
                return true;
            }

            @Override
            public boolean isGlobal() {
                return false;
            }
        }

        public record ConstantAttr(StaticInit init) implements IdentifierAttr {
            @Override
            public boolean isDefinition() {
                return true;
            }

            @Override
            public boolean isGlobal() {
                return false;
            }
        }

        public record EnumeratorAttr(int value) implements IdentifierAttr {
            @Override
            public boolean isDefinition() {
                return true;
            }

            @Override
            public boolean isGlobal() {
                return false;
            }
        }
    }

    private static final String[] GROUPS = {"functions", "statics", "autos", "constants", "enumerators"};

    public void dump(PrintStream out) {
        Map<String, List<Map.Entry<String, Entry>>> groups = new LinkedHashMap<>();
        for (String g : GROUPS) { groups.put(g, new ArrayList<>()); }

        symbolTable.entrySet().stream()
                   .sorted(Map.Entry.comparingByKey())
                   .forEach(e -> groups.get(classify(e.getValue().attr)).add(e));

        int size = symbolTable.size();
        out.println("SymbolTable (" + size + (size == 1 ? " entry)" : " entries)"));

        // 按种类分组输出
        for (var g : groups.entrySet()) {
            List<Map.Entry<String, Entry>> list = g.getValue();
            if (list.isEmpty()) { continue; }

            int nameW = list.stream().mapToInt(e -> e.getKey().length()).max().orElse(0);
            int typeW = list.stream().mapToInt(e -> typeString(e.getValue()).length()).max().orElse(0);

            out.println("  " + g.getKey() + ":");
            for (var e : list) {
                out.printf("    %-" + nameW + "s  %-" + typeW + "s  %s%n",
                           e.getKey(), typeString(e.getValue()), attrString(e.getValue()));
            }
        }
        out.flush();
    }

    private static String classify(Entry.IdentifierAttr attr) {
        if (attr instanceof Entry.FuncAttr) { return "functions"; }
        if (attr instanceof Entry.StaticAttr) { return "statics"; }
        if (attr instanceof Entry.AutoAttr) { return "autos"; }
        if (attr instanceof Entry.ConstantAttr) { return "constants"; }
        if (attr instanceof Entry.EnumeratorAttr) { return "enumerators"; }
        throw new RuntimeException("Unknown entry attribute type: " + attr);
    }

    private static String typeString(Entry e) {
        return e.type == null ? "<?>" : e.type.typename();
    }

    private static String attrString(Entry e) {
        Entry.IdentifierAttr a = e.attr;
        if (a instanceof Entry.FuncAttr f) {
            return (f.defined ? "defined" : "declared") + (f.global ? ", global" : ", static");
        }
        if (a instanceof Entry.StaticAttr s) {
            String d = s.defType instanceof Entry.StaticAttr.Defined def
                ? "defined, init=[" + def.init().size() + " element(s)]"
                : s.defType instanceof Entry.StaticAttr.Tentative
                  ? "tentative"
                  : "no-def";
            return d + (s.global ? ", global" : ", static");
        }
        if (a instanceof Entry.AutoAttr auto) {
            return auto.passByReference ? "by-ref" : "by-val";
        }
        if (a instanceof Entry.ConstantAttr) {
            return "const";
        }
        if (a instanceof Entry.EnumeratorAttr en) {
            return "value=" + en.value;
        }
        return "";
    }
}
