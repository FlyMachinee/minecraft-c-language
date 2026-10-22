package net.flymachine.minecraftclanguage.content.logic.compiler.optimizer.algorithm.simple;

import net.flymachine.minecraftclanguage.content.logic.compiler.optimizer.algorithm.common.FunctionContext;

/**
 * 代表一个简单的遍历优化算法，只需要遍历一次控制流图即可完成操作
 */
@FunctionalInterface
public interface SimpleTraversalPass {
    /**
     * 执行优化算法
     *
     * @param ctx 函数上下文，包含控制流图和符号表等
     * @return 是否对控制流图进行了修改
     */
    boolean runOn(FunctionContext ctx);
}
