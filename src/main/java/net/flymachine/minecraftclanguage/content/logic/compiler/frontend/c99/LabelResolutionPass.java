package net.flymachine.minecraftclanguage.content.logic.compiler.frontend.c99;

import net.flymachine.minecraftclanguage.content.logic.compiler.frontend.c99.ast.AstVisitor;
import net.flymachine.minecraftclanguage.content.logic.compiler.frontend.c99.ast.node.*;
import net.flymachine.minecraftclanguage.content.logic.errorHandle.DiagnosticReporter;

import java.util.*;

/**
 * 一趟扫描检查标签定义和 goto 语句的合法性，并重命名标签使之以函数名为前缀
 * <p>
 * 为switch语句分配唯一标签，将case和default标签与最近的switch进行关联
 * <p>
 * 需要先进行 {@link TypeCheckingPass}
 */
public final class LabelResolutionPass implements AstVisitor<Void> {

    private final DiagnosticReporter reporter;

    public LabelResolutionPass(DiagnosticReporter reporter) {
        this.reporter = reporter;
    }

    private final Map<String, StatementNode.GotoLabelInfo> labelDefinitionMap = new HashMap<>();
    private final Map<String, List<GotoNode>> pendingGotoNodes = new HashMap<>();

    private FunctionDefinitionNode currentFunction;

    private int switchCounter = 0;
    private final Stack<SwitchStatementNode> switchStack = new Stack<>();

    private void enterSwitch(SwitchStatementNode node) {
        node.switchLabel = "switch_" + switchCounter++;
        switchStack.push(node);
    }

    private void exitSwitch() {
        switchStack.pop();
    }

    private SwitchStatementNode getCurrentSwitch() {
        if (switchStack.empty()) {
            return null;
        } else {
            return switchStack.peek();
        }
    }

    @Override
    public Void visit(ProgramNode node) {
        for (ExternalDeclarationNode externalDeclaration : node.extDecls) {
            externalDeclaration.accept(this);
        }
        return null;
    }

    @Override
    public Void visit(FunctionDefinitionNode node) {
        currentFunction = node;
        labelDefinitionMap.clear();
        pendingGotoNodes.clear();
        visit(node.body);
        if (!pendingGotoNodes.isEmpty()) {
            for (Map.Entry<String, List<GotoNode>> entry : pendingGotoNodes.entrySet()) {
                String label = entry.getKey();
                List<GotoNode> gotoNodes = entry.getValue();
                for (GotoNode gotoNode : gotoNodes) {
                    String msg = "label '" + reporter.white(label) + "' used but not defined";
                    reporter.error(gotoNode.gotoLoc, msg);
                }
            }
        }
        return null;
    }

    private void visit(StatementNode node) {
        List<StatementNode.GotoLabelInfo> labels = node.gotoLabels;
        // 标签为反向添加，所以要反向遍历
        for (int i = labels.size() - 1; i >= 0; i--) {
            defineLabel(labels.get(i));
        }

        List<StatementNode.CaseLabelInfo> caseLabels = node.caseLabels;
        for (int i = caseLabels.size() - 1; i >= 0; i--) {
            defineCaseLabel(caseLabels.get(i));
        }

        List<StatementNode.DefaultLabelInfo> defaultLabels = node.defaultLabels;
        for (int i = defaultLabels.size() - 1; i >= 0; i--) {
            defineDefaultLabel(defaultLabels.get(i));
        }
    }

    private void defineLabel(StatementNode.GotoLabelInfo gotoLabelInfo) {
        String id = gotoLabelInfo.label.name;
        StatementNode.GotoLabelInfo definition = labelDefinitionMap.get(id);
        if (definition != null) {
            // 标签重定义
            reporter.error(gotoLabelInfo.label.wholeLoc, "duplicate label '" + reporter.white(id) + "'");
            reporter.note(definition.label.wholeLoc, "previous definition of '" + reporter.white(id) + "'");
        } else {
            // 重命名以函数名开头
            rename(gotoLabelInfo.label);
            labelDefinitionMap.put(id, gotoLabelInfo);

            // 重命名先前使用但未定义的 goto 语句的目标
            List<GotoNode> pendingList = pendingGotoNodes.remove(id);
            if (pendingList != null) {
                gotoLabelInfo.active = true;
                for (GotoNode gotoNode : pendingList) {
                    gotoNode.target.name = gotoLabelInfo.label.name;
                }
            }
        }
    }

    private void defineCaseLabel(StatementNode.CaseLabelInfo caseLabelInfo) {
        SwitchStatementNode switchNode = getCurrentSwitch();
        if (switchNode == null) {
            // 当前没有在switch语句内
            reporter.error(caseLabelInfo.caseLocation, "case label not within a switch statement");
        } else {
            if (!(caseLabelInfo.caseValue instanceof ConstantNode)) {
                reporter.error(caseLabelInfo.caseLocation, "case label does not reduce to an integer constant");
                return;
            }
            // 在switch中，查询当前的case数值是否已定义
            ConstantNode newConstantNode = new ConstantNode(
                caseLabelInfo.caseValue.wholeLoc,
                ((ConstantNode) caseLabelInfo.caseValue).value.castTo(switchNode.exp.expType));
            caseLabelInfo.caseValue = newConstantNode;

            // switch 语句体可拥有任意数量的 case: 标号，只要所有常量表达式的值（在转换到表达式的提升后类型后）各不相同
            // 需要进行常量转换
            // TODO: 若以后添加枚举，则不能直接转换至 BasicType
            long value = newConstantNode.value.toLong().value();
            StatementNode.CaseLabelInfo definition = switchNode.caseValues.get(value);
            if (definition != null) {
                // 已定义
                reporter.error(caseLabelInfo.caseLocation, "duplicate case value");
                reporter.note(definition.caseLocation, "previously used here");
            } else {
                // 无定义，进行定义
                switchNode.caseValues.put(value, caseLabelInfo);
                // 关联当前case标签与switch语句
                caseLabelInfo.switchLabel = switchNode.switchLabel;
            }
        }
    }

    private void defineDefaultLabel(StatementNode.DefaultLabelInfo defaultLabelInfo) {
        SwitchStatementNode switchNode = getCurrentSwitch();
        if (switchNode == null) {
            // 当前没有在switch语句内
            String msg = "'" + reporter.white("default") + "' label not within a switch statement";
            reporter.error(defaultLabelInfo.location, msg);
        } else {
            // 在switch中，查询当前的default是否已定义
            if (switchNode.defaultLabel != null) {
                // 已经定义
                reporter.error(defaultLabelInfo.location, "multiple default labels in one switch");
                reporter.note(switchNode.defaultLabel.location, "this is the first default label");
            } else {
                // 无定义，进行定义
                switchNode.defaultLabel = defaultLabelInfo;
                // 关联当前default标签与switch语句
                defaultLabelInfo.switchLabel = switchNode.switchLabel;
            }
        }
    }

    private int labelRenameCounter = 0;

    private void rename(IdentifierNode identifierNode) {
        identifierNode.name = currentFunction.id.name + "__" + identifierNode.name + "__" + labelRenameCounter++;
    }

    @Override
    public Void visit(ReturnNode node) {
        visit((StatementNode) node);
        return null;
    }

    @Override
    public Void visit(UnaryExpressionNode node) {
        return null;
    }

    @Override
    public Void visit(BinaryExpressionNode node) {
        return null;
    }

    @Override
    public Void visit(DeclarationNode node) {
        return null;
    }

    @Override
    public Void visit(ExpressionStatementNode node) {
        visit((StatementNode) node);
        return null;
    }

    @Override
    public Void visit(NullStatementNode node) {
        visit((StatementNode) node);
        return null;
    }

    @Override
    public Void visit(VariableNode node) {
        return null;
    }

    @Override
    public Void visit(AssignmentNode node) {
        return null;
    }

    @Override
    public Void visit(IncrementDecrementNode node) {
        return null;
    }

    @Override
    public Void visit(IfStatementNode node) {
        visit((StatementNode) node);
        node.thenStmt.accept(this);
        if (node.elseStmt != null) {
            node.elseStmt.accept(this);
        }
        return null;
    }

    @Override
    public Void visit(ConditionalExpressionNode node) {
        return null;
    }

    @Override
    public Void visit(GotoNode node) {
        visit((StatementNode) node);

        String target = node.target.name;
        StatementNode.GotoLabelInfo definition = labelDefinitionMap.get(target);
        if (definition != null) {
            // 有定义，直接重命名
            node.target.name = definition.label.name;
            // 设置标签为活跃
            definition.active = true;
            return null;
        }

        // 无定义，添加至 pending 列表
        List<GotoNode> pendingList = pendingGotoNodes.get(target);
        if (pendingList == null) {
            List<GotoNode> gotoList = new ArrayList<>();
            gotoList.add(node);
            pendingGotoNodes.put(target, gotoList);
        } else {
            pendingList.add(node);
        }
        return null;
    }

    @Override
    public Void visit(CompoundStatementNode node) {
        visit((StatementNode) node);
        for (BlockItemNode blockItemNode : node.blockItems) {
            blockItemNode.accept(this);
        }
        return null;
    }

    @Override
    public Void visit(BreakNode node) {
        visit((StatementNode) node);
        return null;
    }

    @Override
    public Void visit(ContinueNode node) {
        visit((StatementNode) node);
        return null;
    }

    @Override
    public Void visit(WhileLoopNode node) {
        visit((StatementNode) node);
        node.body.accept(this);
        return null;
    }

    @Override
    public Void visit(ForLoopNode node) {
        visit((StatementNode) node);
        node.body.accept(this);
        return null;
    }

    @Override
    public Void visit(SwitchStatementNode node) {
        visit((StatementNode) node);
        enterSwitch(node);
        node.body.accept(this);
        exitSwitch();
        return null;
    }

    @Override
    public Void visit(FunctionCallNode node) {
        return null;
    }

    @Override
    public Void visit(ConstantNode node) {
        return null;
    }

    @Override
    public Void visit(CastExpressionNode node) {
        return null;
    }

    @Override
    public Void visit(AddressOfNode node) {
        return null;
    }

    @Override
    public Void visit(DereferenceNode node) {
        return null;
    }
}
