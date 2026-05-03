package net.flymachine.minecraftclanguage.content.logic.compiler.frontend.c99;

import net.flymachine.minecraftclanguage.content.logger.ConsoleLogger;
import net.flymachine.minecraftclanguage.content.logger.Logger;
import net.flymachine.minecraftclanguage.content.logic.compiler.frontend.c99.ast.AstVisitor;
import net.flymachine.minecraftclanguage.content.logic.compiler.frontend.c99.ast.node.*;
import net.flymachine.minecraftclanguage.content.logic.errorHandle.ErrorHandleUtil;
import net.flymachine.minecraftclanguage.content.logic.errorHandle.SourceFile;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 一趟扫描检查标签定义和 goto 语句的合法性，并重命名标签使之以函数名为前缀
 */
public final class LabelResolutionPass implements AstVisitor<Void> {

    private Logger logger;
    private SourceFile sourceFile;
    private boolean semanticError = false;

    public LabelResolutionPass() {
        this.logger = new ConsoleLogger();
    }

    public boolean hasSemanticError() {
        return semanticError;
    }

    public Logger getLogger() {
        return logger;
    }

    public void setLogger(Logger logger) {
        this.logger = logger;
    }

    public SourceFile getSourceFile() {
        return sourceFile;
    }

    public void setSourceFile(SourceFile sourceFile) {
        this.sourceFile = sourceFile;
    }

    private final Map<String, StatementNode.GotoLabelInfo> labelDefinitionMap = new HashMap<>();
    private final Map<String, List<GotoNode>> pendingGotoNodes = new HashMap<>();

    private FunctionDefinitionNode currentFunction;

    @Override
    public Void visit(ProgramNode node) {
        visit(node.functionDefinition);
        return null;
    }

    @Override
    public Void visit(FunctionDefinitionNode node) {
        currentFunction = node;
        labelDefinitionMap.clear();
        pendingGotoNodes.clear();
        visit(node.body);
        if (!pendingGotoNodes.isEmpty()) {
            semanticError = true;
            for (Map.Entry<String, List<GotoNode>> entry : pendingGotoNodes.entrySet()) {
                String label = entry.getKey();
                List<GotoNode> gotoNodes = entry.getValue();
                for (GotoNode gotoNode : gotoNodes) {
                    String msg =
                        "label '" + logger.formatWithColor(label, Logger.Color.WHITE) + "' used but not defined";
                    ErrorHandleUtil.logErrorWithSourceLine(logger, sourceFile, gotoNode.gotoLocation, msg);
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
    }

    private void defineLabel(StatementNode.GotoLabelInfo gotoLabelInfo) {
        String id = gotoLabelInfo.label.id;
        StatementNode.GotoLabelInfo definition = labelDefinitionMap.get(id);
        if (definition != null) {
            // 标签重定义
            semanticError = true;
            String msg = "duplicate label '" + logger.formatWithColor(id, Logger.Color.WHITE) + "'";
            ErrorHandleUtil.logErrorWithSourceLine(logger, sourceFile, gotoLabelInfo.label.wholeLocation, msg);
            msg = "previous definition of '" + logger.formatWithColor(id, Logger.Color.WHITE) + "'";
            ErrorHandleUtil.logNoteWithSourceLine(logger, sourceFile, definition.label.wholeLocation, msg);
        } else {
            // 重命名以函数名开头
            rename(gotoLabelInfo.label);
            labelDefinitionMap.put(id, gotoLabelInfo);

            // 重命名先前使用但未定义的 goto 语句的目标
            List<GotoNode> pendingList = pendingGotoNodes.remove(id);
            if (pendingList != null) {
                gotoLabelInfo.active = true;
                for (GotoNode gotoNode : pendingList) {
                    gotoNode.target.id = gotoLabelInfo.label.id;
                }
            }
        }
    }

    private void rename(IdentifierNode identifierNode) {
        identifierNode.id = currentFunction.identifier.id + "__" + identifierNode.id;
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
    public Void visit(IdentifierNode node) {
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

        String target = node.target.id;
        StatementNode.GotoLabelInfo definition = labelDefinitionMap.get(target);
        if (definition != null) {
            // 有定义，直接重命名
            node.target.id = definition.label.id;
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
        for (BlockItemNode blockItemNode : node.blockItems) {
            blockItemNode.accept(this);
        }
        return null;
    }
}
