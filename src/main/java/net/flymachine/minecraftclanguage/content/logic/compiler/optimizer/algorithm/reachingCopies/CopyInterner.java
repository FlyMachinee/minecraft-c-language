package net.flymachine.minecraftclanguage.content.logic.compiler.optimizer.algorithm.reachingCopies;

import net.flymachine.minecraftclanguage.content.logic.compiler.ir.operand.TacValue;
import net.flymachine.minecraftclanguage.content.logic.compiler.ir.operand.TacVariable;

import java.util.*;

public final class CopyInterner {
    public record CopyKey(String dst, TacValue src) { }

    private final Map<CopyKey, Integer> keyToIdxMap = new HashMap<>();
    private final List<CopyKey> idxToKeyList = new ArrayList<>();

    // 变量名到 “dst或src为该变量 的copy” 的下标集合的映射
    // 用于到达复制分析的 kill 集合
    private final Map<String, BitSet> varToUseOrDefCopiesMap = new HashMap<>();

    // 变量名到 “dst为该变量 的copy” 的下标集合的映射
    // 用于复制传播
    private final Map<String, BitSet> varToDefCopiesMap = new HashMap<>();

    /**
     * 使用时，请一次性构建，随后使用 indexOf 查询
     */
    public int intern(String dst, TacValue src) {
        CopyKey key = new CopyKey(dst, src);
        Integer i = keyToIdxMap.get(key);
        if (i != null) { return i; }

        i = idxToKeyList.size();
        keyToIdxMap.put(key, i);
        idxToKeyList.add(key);

        varToUseOrDefCopiesMap.computeIfAbsent(dst, k -> new BitSet()).set(i);
        varToDefCopiesMap.computeIfAbsent(dst, k -> new BitSet()).set(i);
        if (src instanceof TacVariable v) {
            varToUseOrDefCopiesMap.computeIfAbsent(v.name, k -> new BitSet()).set(i);
        }
        return i;
    }

    public int indexOf(String dst, TacValue src) {
        return keyToIdxMap.get(new CopyKey(dst, src));
    }

    public CopyKey copyOf(int index) {
        return idxToKeyList.get(index);
    }

    public BitSet relatedCopies(String varName) {
        return varToUseOrDefCopiesMap.getOrDefault(varName, new BitSet());
    }

    public BitSet defCopies(String varName) {
        return varToDefCopiesMap.getOrDefault(varName, new BitSet());
    }

    public int size() {
        return idxToKeyList.size();
    }
}
