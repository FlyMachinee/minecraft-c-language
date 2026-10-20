package net.flymachine.minecraftclanguage.content.logic.compiler.optimizer.algorithm.simple;

import net.flymachine.minecraftclanguage.content.logic.compiler.frontend.c99.SymbolTable;
import net.flymachine.minecraftclanguage.content.logic.compiler.optimizer.cfg.ControlFlowGraph;

/**
 * 代表一个简单的遍历优化算法，只需要遍历一次控制流图即可完成操作
 */
@FunctionalInterface
public interface SimpleTraversalPass {
    /**
     * 执行优化算法
     *
     * @param cfg         控制流图
     * @param symbolTable 符号表
     * @return 是否对控制流图进行了修改
     */
    boolean runOn(ControlFlowGraph cfg, SymbolTable symbolTable);
}
