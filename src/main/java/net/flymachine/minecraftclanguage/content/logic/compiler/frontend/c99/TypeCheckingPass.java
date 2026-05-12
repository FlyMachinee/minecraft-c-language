package net.flymachine.minecraftclanguage.content.logic.compiler.frontend.c99;

import net.flymachine.minecraftclanguage.content.logger.ConsoleLogger;
import net.flymachine.minecraftclanguage.content.logic.compiler.common.type.BasicType;
import net.flymachine.minecraftclanguage.content.logic.compiler.common.type.Type;
import net.flymachine.minecraftclanguage.content.logic.compiler.frontend.c99.ast.AstVisitor;
import net.flymachine.minecraftclanguage.content.logic.compiler.frontend.c99.ast.node.*;
import net.flymachine.minecraftclanguage.content.logic.errorHandle.SourceLocation;

import java.util.HashMap;
import java.util.Map;

public class TypeCheckingPass extends SemanticAnalysePass implements AstVisitor<Void> {

    public TypeCheckingPass() {
        super(new ConsoleLogger());
    }

    private final Map<String, SymbolTableEntry> symbolTable = new HashMap<>();

    public static class SymbolTableEntry {
        IdentifierNode id;
        TypeNode type;
        boolean defined;

        public SymbolTableEntry(IdentifierNode id, TypeNode type, boolean defined) {
            this.id = id;
            this.type = type;
            this.defined = defined;
        }
    }

    @Override
    public Void visit(ProgramNode node) {
        for (ExternalDeclarationNode externalDeclaration : node.declarations) {
            externalDeclaration.accept(this);
        }
        return null;
    }

    @Override
    public Void visit(FunctionDefinitionNode node) {
        // 函数不会被重命名，所以以下 id 信息是正确的
        SymbolTableEntry entry = symbolTable.get(node.identifier.id);
        if (entry != null) {
            if (entry.defined) {
                // 重定义函数
                error();
                String msg = "redefinition of '" + getLogger().white(node.identifier.id) + "'";
                logErrorWithSourceLine(node.identifier.wholeLocation, msg);
                msg = "previous definition of '" + getLogger().white(node.identifier.id) +
                      "' with type '" + getLogger().white(entry.type.getType().toString()) + "'";
                logNoteWithSourceLine(entry.id.wholeLocation, msg);
            } else {
                // 第一次定义
                if (!entry.type.getType().isCompatible(node.functionType.getType())) {
                    // 类型不兼容
                    error();
                    String msg =
                        "conflicting types for '" + getLogger().white(node.identifier.id) + "'; have '" +
                        getLogger().white(node.functionType.getType().toString()) + "'";
                    logErrorWithSourceLine(node.identifier.wholeLocation, msg);
                    msg = "previous declaration of '" + getLogger().white(node.identifier.id) +
                          "' with type '" + getLogger().white(entry.type.getType().toString()) + "'";
                    logNoteWithSourceLine(entry.id.wholeLocation, msg);
                } else {
                    // 更新表项
                    symbolTable.put(node.identifier.id, new SymbolTableEntry(node.identifier, node.functionType, true));
                }
            }
        } else {
            // 第一次定义
            symbolTable.put(node.identifier.id, new SymbolTableEntry(node.identifier, node.functionType, true));
        }
        // 检查参数
        if (!(node.functionType instanceof FunctionTypeNode funcType)) {
            // 不是函数类型
            error();
            String msg = "identifier declared in a function definition shall have a function type; have '" +
                         getLogger().white(node.functionType.getType().toString()) + "'";
            logErrorWithSourceLine(node.identifier.wholeLocation, msg);
        } else {
            // 检查返回值，只能是 int
            if (!(funcType.returnType instanceof BasicTypeNode returnType) ||
                returnType.getType().getKind() != BasicType.Kind.INT) {
                // 返回值类型不合法
                error();
                String msg = "function '" + getLogger().white(node.identifier.id) +
                             "' has invalid return type '" +
                             getLogger().white(funcType.returnType.getType().toString()) + "'";
                logErrorWithSourceLine(node.identifier.wholeLocation, msg);
            }

            // 检查参数类型
            // 到了这里，要么所有参数都具名且不重复，要么只有单独的void参数
            for (int i = 0; i < funcType.parameters.size(); i++) {
                IdentifierNode id = funcType.parameters.get(i);
                TypeNode type = funcType.parameterTypes.get(i);
                if (id != null) {
                    if (!type.getType().isComplete()) {
                        // 不完整类型
                        error();
                        String msg =
                            "parameter '" + getLogger().white(getSourceFile().getByLocation(id.wholeLocation)) +
                            "' has incomplete type '" +
                            getLogger().white(type.getType().toString()) + "'";
                        logErrorWithSourceLine(id.wholeLocation, msg);
                    }
                    symbolTable.put(id.id, new SymbolTableEntry(id, type, false));
                }
            }
        }
        // 检查函数体
        visit(node.body);
        return null;
    }

    @Override
    public Void visit(ReturnNode node) {
        node.expression.accept(this);
        return null;
    }

    @Override
    public Void visit(UnaryExpressionNode node) {
        node.exp.accept(this);
        return null;
    }

    @Override
    public Void visit(BinaryExpressionNode node) {
        node.lhs.accept(this);
        node.rhs.accept(this);
        return null;
    }

    @Override
    public Void visit(DeclarationNode node) {
        // 到了这里，对于变量，不可能出现重定义，defined 为 true
        // 而对于函数，函数的声明不是定义，defined 为 false

        if (!node.type.getType().isComplete()) {
            // 不完整类型
            error();
            String msg =
                "storage size of '" + getLogger().white(getSourceFile().getByLocation(node.identifier.wholeLocation)) +
                "' isn't known; have type '" +
                getLogger().white(node.type.getType().toString()) + "'";
            logErrorWithSourceLine(node.identifier.wholeLocation, msg);
        } else if (node.type instanceof FunctionTypeNode funcType) {
            // 函数类型
            // 检查返回类型，返回类型目前只能是 int
            if (!(funcType.returnType instanceof BasicTypeNode returnType) ||
                returnType.getType().getKind() != BasicType.Kind.INT) {
                // 返回值类型不合法
                error();
                String msg = "function '" + getLogger().white(node.identifier.id) +
                             "' has invalid return type '" +
                             getLogger().white(funcType.returnType.getType().toString()) + "'";
                logErrorWithSourceLine(node.identifier.wholeLocation, msg);
            }
            // 如果已经声明/定义，检查类型是否匹配
            SymbolTableEntry entry = symbolTable.get(node.identifier.id);
            if (entry != null) {
                if (!entry.type.getType().isCompatible(node.type.getType())) {
                    // 类型不匹配
                    error();
                    String msg =
                        "conflicting types for '" + getLogger().white(node.identifier.id) + "'; have '" +
                        getLogger().white(node.type.getType().toString()) + "'";
                    logErrorWithSourceLine(node.identifier.wholeLocation, msg);
                    msg =
                        "previous " + (entry.defined ? "definition" : "declaration") + " of '" +
                        getLogger().white(node.identifier.id) +
                        "' with type '" + getLogger().white(entry.type.getType().toString()) + "'";
                    logNoteWithSourceLine(entry.id.wholeLocation, msg);
                }
            } else {
                symbolTable.put(node.identifier.id, new SymbolTableEntry(node.identifier, node.type, false));
            }

            if (node.initializer != null) {
                // 函数类型不能使用赋值初始化
                error();
                String msg = "function '" + getLogger().white(node.identifier.id) + "' is initialized like a variable";
                logErrorWithSourceLine(node.initializer.wholeLocation, msg);
            }
        } else {
            // 普通变量，直接定义即可
            symbolTable.put(node.identifier.id, new SymbolTableEntry(node.identifier, node.type, true));
        }

        // 检查初始化表达式
        if (node.initializer != null) {
            node.initializer.accept(this);
        }
        return null;
    }

    @Override
    public Void visit(ExpressionStatementNode node) {
        node.expression.accept(this);
        return null;
    }

    @Override
    public Void visit(NullStatementNode node) {
        return null;
    }

    @Override
    public Void visit(IdentifierNode node) {
        // 始终有定义
        SymbolTableEntry entry = symbolTable.get(node.id);
        TypeNode type = entry.type;
        if (type instanceof FunctionTypeNode) {
            // 函数类型不能作为表达式使用
            error();
            String msg = "function used in arithmetic";
            logErrorWithSourceLine(node.wholeLocation, msg);
        } else if (!type.getType().isComplete()) {
            // 不完整类型不能使用
            error();
            String msg = "storage size of '" + getLogger().white(getSourceFile().getByLocation(node.wholeLocation)) +
                         "' isn't known; have type '" +
                         getLogger().white(type.getType().toString()) + "'";
            logErrorWithSourceLine(node.wholeLocation, msg);
        }
        return null;
    }

    @Override
    public Void visit(AssignmentNode node) {
        node.lhs.accept(this);
        node.rhs.accept(this);
        return null;
    }

    @Override
    public Void visit(IncrementDecrementNode node) {
        node.operand.accept(this);
        return null;
    }

    @Override
    public Void visit(IfStatementNode node) {
        node.cond.accept(this);
        node.thenStmt.accept(this);
        if (node.elseStmt != null) {
            node.elseStmt.accept(this);
        }
        return null;
    }

    @Override
    public Void visit(ConditionalExpressionNode node) {
        node.cond.accept(this);
        node.thenExpr.accept(this);
        node.elseExpr.accept(this);
        return null;
    }

    @Override
    public Void visit(GotoNode node) {
        return null;
    }

    @Override
    public Void visit(CompoundStatementNode node) {
        for (BlockItemNode blockItem : node.blockItems) {
            blockItem.accept(this);
        }
        return null;
    }

    @Override
    public Void visit(BreakNode node) {
        return null;
    }

    @Override
    public Void visit(ContinueNode node) {
        return null;
    }

    @Override
    public Void visit(WhileLoopNode node) {
        if (node.isDoWhile) {
            node.body.accept(this);
            node.cond.accept(this);
        } else {
            node.cond.accept(this);
            node.body.accept(this);
        }
        return null;
    }

    @Override
    public Void visit(ForLoopNode node) {
        if (node.init != null) {
            if (node.init instanceof ForInitDeclarationNode forInitDecl) {
                DeclarationNode decl = forInitDecl.declaration;
                if (decl.type instanceof FunctionTypeNode) {
                    // for 初始化语句中不允许声明函数类型
                    error();
                    String msg = "declaration of non-variable '" + getLogger().white(decl.identifier.id) +
                                 "' in for loop initial declaration";
                    logErrorWithSourceLine(decl.wholeLocation, msg);
                }
            }
            node.init.accept(this);
        }
        if (node.cond != null) {
            node.cond.accept(this);
        }
        if (node.step != null) {
            node.step.accept(this);
        }
        node.body.accept(this);
        return null;
    }

    @Override
    public Void visit(SwitchStatementNode node) {
        node.exp.accept(this);
        node.body.accept(this);
        return null;
    }

    @Override
    public Void visit(FunctionCallNode node) {
        if (!(node.function instanceof IdentifierNode id)) {
            // 目前不允许调用函数指针
            error();
            String msg = "function call expression shall have identifier as function designator";
            logErrorWithSourceLine(node.function.wholeLocation, msg);
        } else {
            SymbolTableEntry entry = symbolTable.get(id.id);
            TypeNode type = entry.type;
            if (!(type instanceof FunctionTypeNode funcType)) {
                // 不是函数类型
                error();
                String msg = "called object '" + getLogger().white(getSourceFile().getByLocation(id.wholeLocation)) +
                             "' is not a function or function pointer; have type '" +
                             getLogger().white(type.getType().toString()) + "'";
                logErrorWithSourceLine(id.wholeLocation, msg);
                msg = "declared here";
                logNoteWithSourceLine(entry.id.wholeLocation, msg);
            } else {
                // 检查调用是否合法
                if (funcType.hasNoParameters()) {
                    // 无参数
                    if (!node.arguments.isEmpty()) {
                        // 传递了参数
                        error();
                        String msg = "too many arguments to function '" + getLogger().white(id.id) + "'";
                        logErrorWithSourceLine(node.function.wholeLocation, msg);
                        msg = "declared here";
                        logNoteWithSourceLine(entry.id.wholeLocation, msg);
                    }
                } else {
                    // 有参数
                    if (node.arguments.size() < funcType.parameterTypes.size()) {
                        // 参数不足
                        error();
                        String msg = "too few arguments to function '" + getLogger().white(id.id) + "'";
                        logErrorWithSourceLine(node.function.wholeLocation, msg);
                        msg = "declared here";
                        logNoteWithSourceLine(entry.id.wholeLocation, msg);
                    } else if (node.arguments.size() > funcType.parameterTypes.size()) {
                        // 参数过多
                        error();
                        String msg = "too many arguments to function '" + getLogger().white(id.id) + "'";
                        logErrorWithSourceLine(node.function.wholeLocation, msg);
                        msg = "declared here";
                        logNoteWithSourceLine(entry.id.wholeLocation, msg);
                    } else {
                        // 参数数量正确，检查类型
                        for (int i = 0; i < node.arguments.size(); i++) {
                            // 假设所有表达式类型都是 int
                            Type argType = BasicType.INT;
                            TypeNode paramType = funcType.parameterTypes.get(i);
                            if (!argType.isCompatible(paramType.getType())) {
                                // 参数类型不兼容
                                error();
                                String msg =
                                    "incompatible type for argument " + (i + 1) + " of '" + getLogger().white(id.id) +
                                    "'";
                                logErrorWithSourceLine(node.arguments.get(i).wholeLocation, msg);
                                msg = "expected '" + getLogger().white(paramType.getType().toString()) +
                                      "' but argument is of type '" + getLogger().white(argType.toString()) + "'";
                                logNoteWithSourceLine(
                                    SourceLocation.concat(
                                        funcType.parameterTypes.get(i).getWholeLocation(),
                                        funcType.parameters.get(i).getWholeLocation()),
                                    msg);
                            }
                        }
                    }
                }
            }
        }
        // 检查所有的参数
        for (ExpressionNode arg : node.arguments) {
            arg.accept(this);
        }
        return null;
    }
}
