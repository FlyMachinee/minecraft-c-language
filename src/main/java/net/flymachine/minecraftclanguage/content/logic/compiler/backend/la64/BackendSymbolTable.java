package net.flymachine.minecraftclanguage.content.logic.compiler.backend.la64;

import net.flymachine.minecraftclanguage.content.logic.compiler.backend.la64.highLevel.AsmType;

import java.io.PrintStream;
import java.util.*;

public final class BackendSymbolTable {

    public BackendSymbolTable() { }

    public Entry get(String name) {
        return symbolTable.get(name);
    }

    public void put(String name, Entry symbol) {
        symbolTable.put(name, symbol);
    }

    public Entry remove(String name) {
        return symbolTable.remove(name);
    }

    public boolean contains(String name) {
        return symbolTable.containsKey(name);
    }

    public void clear() {
        symbolTable.clear();
    }

    public Collection<Entry> getEntries() {
        return symbolTable.values();
    }

    private final Map<String, Entry> symbolTable = new HashMap<>();

    public sealed interface Entry permits ObjectEntry, FuncEntry { }

    public record ObjectEntry(AsmType asmType, boolean isStatic, boolean isConstant) implements Entry { }

    public record FuncEntry(boolean defined, boolean returnOnStack) implements Entry { }

    private static final String[] GROUPS = {"functions", "statics", "autos"};

    public void dump(PrintStream out) {
        Map<String, List<Map.Entry<String, Entry>>> groups = new LinkedHashMap<>();
        for (String g : GROUPS) { groups.put(g, new ArrayList<>()); }

        symbolTable.entrySet().stream()
                   .sorted(Map.Entry.comparingByKey())
                   .forEach(e -> groups.get(classify(e.getValue())).add(e));

        int size = symbolTable.size();
        out.println("BackendSymbolTable (" + size + (size == 1 ? " entry)" : " entries)"));

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

    private static String classify(Entry e) {
        if (e instanceof FuncEntry) { return "functions"; }
        if (e instanceof ObjectEntry o) { return o.isStatic() ? "statics" : "autos"; }
        throw new RuntimeException("Unknown entry type: " + e);
    }

    private static String typeString(Entry e) {
        if (e instanceof ObjectEntry o) {
            return o.asmType() == null ? "<?>" : o.asmType().toString();
        }
        return "FUNC";
    }

    private static String attrString(Entry e) {
        if (e instanceof FuncEntry f) {
            return (f.defined() ? "defined" : "declared")
                   + (f.returnOnStack() ? ", return-on-stack" : "");
        }
        if (e instanceof ObjectEntry o) {
            return (o.isConstant() ? "constexpr, " : "")
                   + (o.isStatic() ? "static" : "auto");
        }
        return "";
    }
}
