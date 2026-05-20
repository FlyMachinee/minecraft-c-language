package net.flymachine.minecraftclanguage.content.logic.compiler.backend.la64;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

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

    public record ObjectEntry(boolean isStatic) implements Entry { }

    public record FuncEntry(boolean defined) implements Entry { }
}
