package net.flymachine.minecraftclanguage.content.logic.compiler.frontend.c99.ast.node;

import net.flymachine.minecraftclanguage.content.logic.compiler.frontend.c99.ast.StatementVisitor;
import net.flymachine.minecraftclanguage.content.logic.errorHandle.SourceLocation;

import java.util.ArrayList;
import java.util.List;
import java.util.StringJoiner;

public abstract class StatementNode extends AstNode {
    public List<GotoLabelInfo> gotoLabels = new ArrayList<>();
    public List<DefaultLabelInfo> defaultLabels = new ArrayList<>();
    public List<CaseLabelInfo> caseLabels = new ArrayList<>();

    protected StatementNode(SourceLocation wholeLocation) {
        super(wholeLocation);
    }

    public boolean isLabeled() {
        return !gotoLabels.isEmpty() || !defaultLabels.isEmpty() || !caseLabels.isEmpty();
    }

    public void genFormatedStringForLabels(StringBuilder stringBuilder) {
        StringJoiner stringJoiner = new StringJoiner(", ", "labels=[", "]");
        for (GotoLabelInfo info : gotoLabels) {
            stringJoiner.add(info.label.id);
        }
        for (DefaultLabelInfo info : defaultLabels) {
            stringJoiner.add("default@" + info.switchLabel);
        }
        for (CaseLabelInfo info : caseLabels) {
            stringJoiner.add("case " + info.caseValue + "@" + info.switchLabel);
        }
        stringBuilder.append(stringJoiner);
    }

    public abstract void accept(StatementVisitor visitor);

    /**
     * 计算该语句及其子语句是否包含活跃的标签
     *
     * @return 如果包含活跃的标签，则返回 {@code true}；否则返回 {@code false}
     */
    public boolean containsActiveLabel() {
        if (!defaultLabels.isEmpty() || !caseLabels.isEmpty()) {
            return true;
        }
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

    public static class DefaultLabelInfo {
        public SourceLocation location;
        public String switchLabel;

        public DefaultLabelInfo(SourceLocation location) {
            this.location = location;
        }
    }

    public static class CaseLabelInfo {
        public SourceLocation caseLocation;
        public ExpressionNode caseValue;
        public String switchLabel;

        public CaseLabelInfo(SourceLocation caseLocation, ExpressionNode caseValue) {
            this.caseLocation = caseLocation;
            this.caseValue = caseValue;
        }
    }
}
