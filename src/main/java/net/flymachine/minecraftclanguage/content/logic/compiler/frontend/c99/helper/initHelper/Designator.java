package net.flymachine.minecraftclanguage.content.logic.compiler.frontend.c99.helper.initHelper;

import net.flymachine.minecraftclanguage.content.logic.compiler.common.type.Type;
import net.flymachine.minecraftclanguage.content.logic.compiler.frontend.c99.ast.node.CompoundInitializerNode;
import net.flymachine.minecraftclanguage.content.logic.compiler.frontend.c99.ast.node.InitializerNode;

sealed interface Designator permits ArrayDesignator {
    /**
     * 获取该指代符所管理的聚合类型
     *
     * @return 该指代符所管理的聚合类型
     */
    Type type();

    /**
     * 获取该指代符所指向的类型
     *
     * @return 该指代符所指向的类型
     */
    Type subtype();

    /**
     * 将指代符移动到下一个位置
     *
     * @return 如果移动成功返回 true，否则返回 false
     */
    boolean next();

    /**
     * 将指代符应用到完整初始化器上，返回应用后的子初始化器。比如，对于数组指代符 {@code [2]} 和初始化器：
     * <blockquote><pre>
     *     Compound([
     *          Compound([Single(1), Single(2)]),
     *          Compound([Single(3), Single(4)]),
     *          Compound([Single(5), Single(6)]),
     *          Compound([Single(7), Single(8)])
     *     ])
     * </pre></blockquote><p>
     * 将得到该初始化器的第 2 个元素：
     * <blockquote><pre>
     *     Compound([Single(5), Single(6)])
     * </pre></blockquote>
     *
     * @param initializer 要应用指代符的完整初始化器
     * @return 应用指代符后的子初始化器
     */
    InitializerNode apply(CompoundInitializerNode initializer);


    /**
     * 将指代符应用到完整初始化器上，替换应用后的子初始化器为新的值。要求该指代符所管理的聚合类型与新值的类型相同。
     * <p>
     * 比如，对于数组指代符 {@code [2]} 和初始化器：
     * <blockquote><pre>
     *     Compound([
     *          Compound([Single(1), Single(2)]),
     *          Compound([Single(3), Single(4)]),
     *          Compound([Single(5), Single(6)]),
     *          Compound([Single(7), Single(8)])
     *     ])
     * </pre></blockquote><p>
     * 将该初始化器的第 2 个元素替换为新的值：
     * <blockquote><pre>
     *     Compound([
     *          Compound([Single(1), Single(2)]),
     *          Compound([Single(3), Single(4)]),
     *          newValue,
     *          Compound([Single(7), Single(8)])
     *     ])
     * </pre></blockquote>
     *
     * @param initializer 要应用指代符的完整初始化器
     * @param newValue    新的子初始化器值
     */
    void apply(CompoundInitializerNode initializer, InitializerNode newValue);
}
