package net.flymachine.minecraftclanguage.content.logic.compiler.frontend.c99.helper.initHelper;

import net.flymachine.minecraftclanguage.content.logic.compiler.common.type.Type;
import net.flymachine.minecraftclanguage.content.logic.compiler.common.type.UnionType;
import net.flymachine.minecraftclanguage.content.logic.compiler.common.type.record.Field;
import net.flymachine.minecraftclanguage.content.logic.compiler.frontend.c99.ast.node.*;

import java.util.List;

public final class UnionMemberDesignator implements Designator {
    private final long memberIndex;
    private final UnionType type;
    private boolean oob = false;

    public UnionMemberDesignator(String member, UnionType type) {
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

    public UnionMemberDesignator(long memberIndex, UnionType type) {
        this.type = type;
        if (memberIndex < 0 || memberIndex >= type.info().fields().size()) {
            oob = true;
        }
        this.memberIndex = memberIndex;
    }

    public UnionMemberDesignator(UnionType type) {
        this.type = type;
        this.memberIndex = 0;
    }

    @Override
    public UnionType type() {
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
        oob = true;
        return false;
    }

    @Override
    public InitializerNode apply(CompoundInitializerNode initializer) {
        return initializer.inits.get(0).initializer;
    }

    @Override
    public void apply(CompoundInitializerNode initializer, InitializerNode newValue) {
        Field member = type.info().fields().get((int) memberIndex);
        IdentifierNode memberId = new IdentifierNode(newValue.getWholeLocation(), member.name);
        MemberDesignatorNode memberDesignator =
            new MemberDesignatorNode(newValue.getWholeLocation(), memberId);

        initializer.inits.set(0, new DesignationInitializerNode(
            newValue.getWholeLocation(), List.of(memberDesignator), newValue));
    }
}
