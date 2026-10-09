package net.flymachine.minecraftclanguage.content.logic.compiler.frontend.c99.helper.initHelper;

import net.flymachine.minecraftclanguage.content.logic.compiler.common.type.StructType;
import net.flymachine.minecraftclanguage.content.logic.compiler.common.type.Type;
import net.flymachine.minecraftclanguage.content.logic.compiler.frontend.c99.ast.node.CompoundInitializerNode;
import net.flymachine.minecraftclanguage.content.logic.compiler.frontend.c99.ast.node.DesignationInitializerNode;
import net.flymachine.minecraftclanguage.content.logic.compiler.frontend.c99.ast.node.InitializerNode;

import java.util.List;

public final class MemberDesignator implements Designator {
    private long memberIndex;
    private final StructType type;
    private boolean oob = false;

    public MemberDesignator(String member, StructType type) {
        this.type = type;
        int index = -1;
        for (int i = 0; i < type.info().fields().size(); i++) {
            if (type.info().fields().get(i).name.equals(member)) {
                index = i;
                break;
            }
        }
        if (index == -1) {
            oob = true;
        }
        this.memberIndex = index;
    }

    public MemberDesignator(long memberIndex, StructType type) {
        this.type = type;
        if (memberIndex < 0 || memberIndex >= type.info().fields().size()) {
            oob = true;
        }
        this.memberIndex = memberIndex;
    }

    public MemberDesignator(StructType type) {
        this.type = type;
        this.memberIndex = 0;
    }

    @Override
    public Type type() {
        return type;
    }

    @Override
    public Type subtype() {
        return type.info().fields().get((int) memberIndex).type;
    }

    @Override
    public boolean next() {
        if (oob) {
            throw new IndexOutOfBoundsException();
        }

        if (memberIndex + 1 < type.info().fields().size()) {
            memberIndex++;
            return true;
        }
        oob = true;
        return false;
    }

    @Override
    public InitializerNode apply(CompoundInitializerNode initializer) {
        return initializer.inits.get((int) memberIndex).initializer;
    }

    @Override
    public void apply(CompoundInitializerNode initializer, InitializerNode newValue) {
        initializer.inits.set(
            (int) memberIndex, new DesignationInitializerNode(newValue.getWholeLocation(), List.of(), newValue));
    }
}
