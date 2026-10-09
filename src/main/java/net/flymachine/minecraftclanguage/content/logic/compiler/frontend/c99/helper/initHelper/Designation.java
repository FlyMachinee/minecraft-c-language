package net.flymachine.minecraftclanguage.content.logic.compiler.frontend.c99.helper.initHelper;

import net.flymachine.minecraftclanguage.content.logic.compiler.common.type.ArrayType;
import net.flymachine.minecraftclanguage.content.logic.compiler.common.type.StructType;
import net.flymachine.minecraftclanguage.content.logic.compiler.common.type.Type;
import net.flymachine.minecraftclanguage.content.logic.compiler.frontend.c99.ast.node.CompoundInitializerNode;
import net.flymachine.minecraftclanguage.content.logic.compiler.frontend.c99.ast.node.InitializerNode;

import java.util.ArrayList;
import java.util.List;

final class Designation {
    private final List<Designator> designators;

    public Designation(Type type) {
        designators = buildDesignator(type);
    }

    public Designation(List<Designator> designators) {
        this.designators = designators;
    }

    private static List<Designator> buildDesignator(Type type) {
        if (!type.isAggregate()) {
            return new ArrayList<>();
        } else {
            List<Designator> designators = new ArrayList<>();
            if (type instanceof ArrayType arrayType) {
                designators.add(new ArrayDesignator(arrayType));
            } else if (type instanceof StructType structType) {
                designators.add(new MemberDesignator(structType));
            } else {
                throw new IllegalArgumentException("Unknown aggregate type: " + type);
            }
            return designators;
        }
    }

    private static List<Designator> buildDesignators(Type type) {
        if (!type.isAggregate()) {
            return new ArrayList<>();
        } else {
            List<Designator> designators = new ArrayList<>();
            Type currentType = type;
            while (currentType.isAggregate()) {
                if (currentType instanceof ArrayType arrayType) {
                    designators.add(new ArrayDesignator(arrayType));
                    currentType = arrayType.elementType();
                } else if (currentType instanceof StructType structType) {
                    designators.add(new MemberDesignator(structType));
                    currentType = structType.info().fields().get(0).type;
                } else {
                    throw new IllegalArgumentException("Unknown aggregate type: " + currentType);
                }
            }
            return designators;
        }
    }

    public void next() {
        for (int i = designators.size() - 1; i >= 0; i--) {
            if (designators.get(i).next()) {
                return;
            } else {
                designators.remove(i);
            }
        }
    }

    public void expand() {
        if (designators.isEmpty()) {
            throw new IllegalStateException("Cannot expand an empty designation");
        }
        Designator last = designators.get(designators.size() - 1);
        if (last.subtype().isAggregate()) {
            designators.addAll(buildDesignators(last.subtype()));
        }
    }

    public boolean isEmpty() {
        return designators.isEmpty();
    }

    public Type subtype() {
        if (designators.isEmpty()) {
            throw new IllegalStateException("Designation is empty");
        }
        return designators.get(designators.size() - 1).subtype();
    }

    public List<Designator> getDesignators() {
        return designators;
    }

    /**
     * 将指代符序列应用到完整初始化器上，返回应用后的子初始化器。比如，对于指代符序列 {@code [2][0]} 和初始化器：
     * <blockquote><pre>
     *     Compound([
     *          Compound([Single(1), Single(2)]),
     *          Compound([Single(3), Single(4)]),
     *          Compound([Single(5), Single(6)]),
     *          Compound([Single(7), Single(8)])
     *     ])
     * </pre></blockquote><p>
     * 将得到该初始化器的第 2 个元素的第 0 个元素：
     * <blockquote><pre>
     *     Single(5)
     * </pre></blockquote>
     *
     * @param initializer 要应用指代符的完整初始化器
     * @return 应用指代符后的子初始化器
     */
    public InitializerNode fetch(CompoundInitializerNode initializer) {
        InitializerNode current = initializer;
        for (Designator designator : designators) {
            if (current instanceof CompoundInitializerNode compound) {
                current = designator.apply(compound);
            } else {
                throw new IllegalArgumentException("Cannot apply designator to non-compound initializer");
            }
        }
        return current;
    }

    /**
     * 将指代符序列应用到完整初始化器上，替换应用后的子初始化器为新的值。要求该指代符所管理的聚合类型与新值的类型相同。
     *
     * @param initializer 要应用指代符的完整初始化器
     * @param newValue    新的子初始化器值
     */
    public void store(CompoundInitializerNode initializer, InitializerNode newValue) {
        InitializerNode current = initializer;
        for (int i = 0; i < designators.size(); i++) {
            Designator designator = designators.get(i);
            if (current instanceof CompoundInitializerNode compound) {
                if (i == designators.size() - 1) {
                    designator.apply(compound, newValue);
                } else {
                    current = designator.apply(compound);
                }
            } else {
                throw new IllegalArgumentException("Cannot apply designator to non-compound initializer");
            }
        }
    }

}
