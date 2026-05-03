package net.flymachine.minecraftclanguage.content.logic.compiler.frontend.c99.ast.node;

import net.flymachine.minecraftclanguage.content.logic.errorHandle.SourceLocation;

import java.util.ArrayList;
import java.util.List;
import java.util.StringJoiner;

public abstract class StatementNode extends BlockItemNode {
    public List<GotoLabelInfo> gotoLabels = new ArrayList<>();

    protected StatementNode(SourceLocation wholeLocation) {
        super(wholeLocation);
    }

    public void genFormatedStringForGotoLabels(StringBuilder stringBuilder) {
        StringJoiner stringJoiner = new StringJoiner(", ", "labels=[", "]");
        for (GotoLabelInfo info : gotoLabels) {
            stringJoiner.add(info.label.id);
        }
        stringBuilder.append(stringJoiner);
    }

    /**
     * 计算该语句及其子语句是否包含活跃的 goto 标签
     *
     * @return 如果包含活跃的 goto 标签，则返回 {@code true}；否则返回 {@code false}
     */
    public boolean containsActiveGotoLabel() {
        for (GotoLabelInfo info : gotoLabels) {
            if (info.active) {
                return true;
            }
        }
        return false;
    }

    public static class GotoLabelInfo {
        public IdentifierNode label;

        /**
         * 是否活跃，即是否有指向该标签的 goto 语句
         */
        public boolean active;

        public GotoLabelInfo(IdentifierNode label, boolean active) {
            this.label = label;
            this.active = active;
        }

        public GotoLabelInfo(IdentifierNode label) {
            this.label = label;
            this.active = false;
        }
    }
}
