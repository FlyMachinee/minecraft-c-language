package net.flymachine.minecraftclanguage.content.logic.compiler.optimizer.algorithm.common;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 把变量名映射到从 0 开始的下标，便于用 BitSet 表示变量集合
 */
public final class VariableInterner {
    private final Map<String, Integer> varToIdxMap = new HashMap<>();
    private final List<String> idxToVarList = new ArrayList<>();

    public int intern(String varName) {
        return varToIdxMap.computeIfAbsent(varName, name -> {
            int index = idxToVarList.size();
            idxToVarList.add(name);
            return index;
        });
    }

    public String nameOf(int index) {
        return idxToVarList.get(index);
    }

    public int size() {
        return idxToVarList.size();
    }
}
