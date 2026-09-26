package net.flymachine.minecraftclanguage.content.logic.compiler.frontend.c99.helper.initHelper;

import net.flymachine.minecraftclanguage.content.logic.compiler.common.type.ArrayType;
import net.flymachine.minecraftclanguage.content.logic.compiler.common.type.Type;
import net.flymachine.minecraftclanguage.content.logic.compiler.frontend.c99.ast.node.CompoundInitializerNode;
import net.flymachine.minecraftclanguage.content.logic.compiler.frontend.c99.ast.node.DesignationInitializerNode;
import net.flymachine.minecraftclanguage.content.logic.compiler.frontend.c99.ast.node.InitializerNode;

import java.util.List;

final class ArrayDesignator implements Designator {
    private long index;
    private final ArrayType type;

    public ArrayDesignator(long index, ArrayType type) {
        this.index = index;
        this.type = type;
    }

    public ArrayDesignator(ArrayType type) {
        this(0, type);
    }

    @Override
    public ArrayType type() {
        return type;
    }

    @Override
    public Type subtype() {
        return type.elementType();
    }

    @Override
    public boolean next() {
        long size = type.size().value();
        if (size <= 0 || index + 1 < size) {
            index++;
            return true;
        }
        return false;
    }

    @Override
    public InitializerNode apply(CompoundInitializerNode initializer) {
        return initializer.inits.get((int) index).initializer;
    }

    @Override
    public void apply(CompoundInitializerNode initializer, InitializerNode newValue) {
        initializer.inits.set(
            (int) index, new DesignationInitializerNode(newValue.getWholeLocation(), List.of(), newValue));
    }

    public long index() {
        return index;
    }
}
