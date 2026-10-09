package net.flymachine.minecraftclanguage.content.logic.compiler.frontend.c99.helper.scope;

import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Stack;

public final class ScopeStack<K, V> {

    private final Stack<Map<K, V>> st = new Stack<>();

    public ScopeStack() { st.push(new HashMap<>()); }

    public void enterScope() { st.push(new HashMap<>()); }

    public void exitScope() { st.pop(); }

    public void pushScope(@NotNull Map<K, V> scope) { st.push(scope); }

    public Map<K, V> popScope() { return st.pop(); }

    public boolean inGlobalScope() { return st.size() == 1; }

    public boolean declaredInCurrentScope(@NotNull K key) { return st.peek().containsKey(key); }

    public Optional<V> declarationInCurrentScope(@NotNull K key) { return Optional.ofNullable(st.peek().get(key)); }

    public boolean declaredInScope(@NotNull K key) {
        for (int i = st.size() - 1; i >= 0; i--) {
            if (st.get(i).containsKey(key)) {
                return true;
            }
        }
        return false;
    }

    public Optional<V> declarationOf(@NotNull K key) {
        for (int i = st.size() - 1; i >= 0; i--) {
            Map<K, V> scope = st.get(i);
            if (scope.containsKey(key)) {
                return Optional.of(scope.get(key));
            }
        }
        return Optional.empty();
    }

    public void declare(K key, @NotNull V value) {
        st.peek().put(key, value);
    }
}
