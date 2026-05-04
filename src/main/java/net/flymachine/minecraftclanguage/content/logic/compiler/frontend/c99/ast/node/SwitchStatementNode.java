package net.flymachine.minecraftclanguage.content.logic.compiler.frontend.c99.ast.node;

import it.unimi.dsi.fastutil.ints.Int2ObjectArrayMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import net.flymachine.minecraftclanguage.content.logic.compiler.frontend.c99.ast.AstVisitor;
import net.flymachine.minecraftclanguage.content.logic.errorHandle.SourceLocation;

public class SwitchStatementNode extends StatementNode {
    public ExpressionNode exp;
    public StatementNode body;
    public String switchLabel;
    public Int2ObjectMap<CaseLabelInfo> caseValues = new Int2ObjectArrayMap<>();
    public DefaultLabelInfo defaultLabel;

    public SwitchStatementNode(ExpressionNode exp, StatementNode body) {
        super(SourceLocation.concat(exp.wholeLocation, body.wholeLocation));
        this.exp = exp;
        this.body = body;
    }

    @Override
    public <T> T accept(AstVisitor<T> visitor) {
        return visitor.visit(this);
    }

    @Override
    public void genFormattedString(StringBuilder stringBuilder, int indentLevel, boolean indentFirstLine) {
        if (indentFirstLine) { stringBuilder.append("  ".repeat(indentLevel)); }
        stringBuilder.append("SwitchStatementNode(\n");
        if (switchLabel != null) {
            stringBuilder.append("  ".repeat(indentLevel + 1)).append("label=").append(switchLabel).append(",\n");
        }
        stringBuilder.append("  ".repeat(indentLevel + 1)).append("exp=");
        exp.genFormattedString(stringBuilder, indentLevel + 1, false);
        stringBuilder.append("  ".repeat(indentLevel + 1)).append("body=");
        body.genFormattedString(stringBuilder, indentLevel + 1, false);
        stringBuilder.append("  ".repeat(indentLevel)).append(")\n");
    }
}
