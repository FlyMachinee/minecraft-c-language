package net.flymachine.minecraftclanguage.content.logic.compiler.frontend.c99;

import net.flymachine.minecraftclanguage.content.logger.ConsoleLogger;
import net.flymachine.minecraftclanguage.content.logger.Logger;
import net.flymachine.minecraftclanguage.content.logic.compiler.common.AssignmentOperator;
import net.flymachine.minecraftclanguage.content.logic.compiler.common.BinaryOperator;
import net.flymachine.minecraftclanguage.content.logic.compiler.common.StorageClassSpecifier;
import net.flymachine.minecraftclanguage.content.logic.compiler.common.UnaryOperator;
import net.flymachine.minecraftclanguage.content.logic.compiler.common.type.BasicType;
import net.flymachine.minecraftclanguage.content.logic.compiler.frontend.antlr.C99Parser;
import net.flymachine.minecraftclanguage.content.logic.compiler.frontend.antlr.C99ParserBaseVisitor;
import net.flymachine.minecraftclanguage.content.logic.compiler.frontend.c99.ast.node.*;
import net.flymachine.minecraftclanguage.content.logic.errorHandle.ErrorHandleUtil;
import net.flymachine.minecraftclanguage.content.logic.errorHandle.SourceFile;
import net.flymachine.minecraftclanguage.content.logic.errorHandle.SourceLocation;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.tree.TerminalNode;
import org.jetbrains.annotations.Nullable;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

/**
 * 将生成的语法树转换为自定义 AST
 * 检查部分语义错误
 */
public final class AstBuilderVisitor extends C99ParserBaseVisitor<AstNode> {

    // 从 SemanticAnalysePass 复制而来，因为 Java 不支持多继承，不知道怎么改才能更好
    private Logger logger;
    private SourceFile sourceFile;
    private boolean semanticError = false;

    public AstBuilderVisitor(Logger logger, SourceFile sourceFile) {
        this.logger = logger;
        this.sourceFile = sourceFile;
    }

    public AstBuilderVisitor(Logger logger) {
        this.logger = logger;
    }

    public AstBuilderVisitor() {
        this.logger = new ConsoleLogger();
    }

    public boolean hasSemanticError() {
        return semanticError;
    }

    public void setSemanticError(boolean semanticError) {
        this.semanticError = semanticError;
    }

    private void error() {
        semanticError = true;
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

    private void logErrorWithSourceLine(SourceLocation sourceLocation, String message) {
        ErrorHandleUtil.logErrorWithSourceLine(getLogger(), getSourceFile(), sourceLocation, message);
    }

    private void logNoteWithSourceLine(SourceLocation sourceLocation, String message) {
        ErrorHandleUtil.logNoteWithSourceLine(getLogger(), getSourceFile(), sourceLocation, message);
    }

    private static SourceLocation getSourceLocation(ParserRuleContext ctx) {
        Token startToken = ctx.getStart();
        Token endToken = ctx.getStop();
        int line = startToken.getLine();
        int column = startToken.getCharPositionInLine();
        return new SourceLocation(line, column, startToken.getStartIndex(), endToken.getStopIndex());
    }

    private static SourceLocation getSourceLocation(TerminalNode terminalNode) {
        Token token = terminalNode.getSymbol();
        int line = token.getLine();
        int column = token.getCharPositionInLine();
        return new SourceLocation(line, column, token.getStartIndex(), token.getStopIndex());
    }

    @Override
    public ProgramNode visitCompilationUnit(C99Parser.CompilationUnitContext ctx) {
        return visitTranslationUnit(ctx.translationUnit());
    }

    @Override
    public ProgramNode visitTranslationUnit(C99Parser.TranslationUnitContext ctx) {
        List<ExternalDeclarationNode> externalDeclarations = new ArrayList<>();
        for (C99Parser.ExternalDeclarationContext externalDeclarationContext : ctx.externalDeclaration()) {
            externalDeclarations.add((ExternalDeclarationNode) visit(externalDeclarationContext));
        }
        return new ProgramNode(getSourceLocation(ctx), externalDeclarations);
    }

    @Override
    public FunctionDefinitionNode visitFunctionDefinition(C99Parser.FunctionDefinitionContext ctx) {
        TypeAndSpecifiers typeAndSpecifiers = parseDeclarationSpecifiers(ctx.declarationSpecifiers());
        TypeNode baseType = typeAndSpecifiers.t;
        boolean isBaseTypeError = baseType == null;
        if (isBaseTypeError) {
            // 返回值无类型
            baseType = new BasicTypeNode(null, BasicType.INT);
        }
        DeclarationLikeResult res = parseFromDeclarator(baseType, ctx.declarator());
        if (isBaseTypeError) {
            error();
            String msg =
                "type defaults to '" + logger.white("int") + "' in declaration of '" + logger.white(res.id.id) + "'";
            logErrorWithSourceLine(res.id.wholeLoc, msg);
        }
        CompoundStatementNode body = (CompoundStatementNode) visit(ctx.compoundStatement());
        return new FunctionDefinitionNode(getSourceLocation(ctx), res.id, res.t, typeAndSpecifiers.storageClass, body);
    }

    private record DeclarationLikeResult(TypeNode t, IdentifierNode id) { }

    private record TypeAndSpecifiers(TypeNode t, @Nullable StorageClassSpecifierNode storageClass) { }

    private class TypeCombinationHelper {
        BasicType t;
        SourceLocation loc;
        int nonLongCount = 0;
        int longCount = 0;

        void append(String typeSpecifierName, SourceLocation loc) {
            BasicType newType = BasicType.fromString(typeSpecifierName);
            if (this.t == null) {
                this.t = newType;
                this.loc = loc;
                if (newType == BasicType.LONG) {
                    longCount++;
                } else {
                    nonLongCount++;
                }
            } else {
                if (newType == BasicType.LONG) {
                    switch (this.t) {
                        case INT -> {
                            this.t = BasicType.LONG;
                        }
                        case LONG -> {
                            if (longCount >= 1) {
                                error();
                                String msg = "'" + logger.white("long long") + "' is too long";
                                logErrorWithSourceLine(loc, msg);
                            }
                        }
                        case VOID -> {
                            error();
                            String msg = "both '" + logger.white("long") + "' and '" + logger.white("void") +
                                         "' in declaration specifiers";
                            logErrorWithSourceLine(loc, msg);
                        }
                    }
                    ++longCount;
                } else {
                    // 新增为非 long
                    if (nonLongCount > 0) {
                        error();
                        String msg = "two or more data types in declaration specifiers";
                        logErrorWithSourceLine(loc, msg);
                    } else {
                        // nonLongCount == 0，this.t 只可能是 long
                        if (newType == BasicType.VOID) {
                            error();
                            String msg = "both '" + logger.white("long") + "' and '" + logger.white("void") +
                                         "' in declaration specifiers";
                            logErrorWithSourceLine(loc, msg);
                        }
                    }
                    ++nonLongCount;
                }
                this.loc = SourceLocation.concat(this.loc, loc);
            }
        }
    }

    private TypeAndSpecifiers parseDeclarationSpecifiers(C99Parser.DeclarationSpecifiersContext ctx) {
        // declarationSpecifiers // rewrote
        //     : declarationSpecifier+
        //     ;
        // declarationSpecifier // added
        //     : storageClassSpecifier
        //     | typeSpecifier
        //     ;
        boolean reportedMultipleStorageClasses = false;
        StorageClassSpecifierNode storageClassNode = null;

        // 类型翻译
        TypeCombinationHelper helper = new TypeCombinationHelper();

        for (var specifierCtx : ctx.declarationSpecifier()) {
            if (specifierCtx.typeSpecifier() != null) {
                String typeSpecifierText = specifierCtx.typeSpecifier().getText();
                helper.append(typeSpecifierText, getSourceLocation(specifierCtx.typeSpecifier()));
            } else if (specifierCtx.storageClassSpecifier() != null) {
                if (storageClassNode == null) {
                    StorageClassSpecifier storageClass =
                        StorageClassSpecifier.fromString(specifierCtx.storageClassSpecifier().getText());
                    storageClassNode = new StorageClassSpecifierNode(
                        getSourceLocation(specifierCtx.storageClassSpecifier()), storageClass);
                } else if (!reportedMultipleStorageClasses) {
                    error();
                    String msg = "multiple storage classes in declaration specifiers";
                    logErrorWithSourceLine(getSourceLocation(specifierCtx.storageClassSpecifier()), msg);
                    reportedMultipleStorageClasses = true;
                }
            }
        }
        return new TypeAndSpecifiers(new BasicTypeNode(helper.loc, helper.t), storageClassNode);
    }

    private DeclarationLikeResult parseFromDeclarator(TypeNode baseType, C99Parser.DeclaratorContext ctx) {
        var directDeclarator = ctx.directDeclarator();
        return parseFromDirectDeclarator(baseType, directDeclarator);
    }

    private DeclarationLikeResult parseFromDirectDeclarator(TypeNode baseType, C99Parser.DirectDeclaratorContext ctx) {
        // directDeclarator -> Identifier
        if (ctx.Identifier() != null) {
            String name = ctx.Identifier().getText();
            SourceLocation nameLocation = getSourceLocation(ctx.Identifier());
            return new DeclarationLikeResult(baseType, new IdentifierNode(nameLocation, name));
        }

        // directDeclarator -> LeftParen declarator RightParen
        if (ctx.declarator() != null) {
            return parseFromDeclarator(baseType, ctx.declarator());
        }

        // directDeclarator -> directDeclarator LeftParen parameterTypeList RightParen
        if (ctx.directDeclarator() != null) {
            // 递归处理左侧
            DeclarationLikeResult inner = parseFromDirectDeclarator(baseType, ctx.directDeclarator());
            TypeNode t = inner.t;
            IdentifierNode id = inner.id;

            if (ctx.parameterTypeList() != null) {
                // directDeclarator -> directDeclarator LeftParen parameterTypeList RightParen
                // 构造函数类型
                FunctionTypeNode funcType = parseFromParameterTypeList(t, ctx.parameterTypeList(), ctx.RightParen());
                return new DeclarationLikeResult(funcType, id);
            }
        }

        throw new RuntimeException("Unknown direct declarator: " + ctx.getText());
    }

    private TypeNode parseFromAbstractDeclarator(TypeNode baseType, C99Parser.AbstractDeclaratorContext ctx) {
        var directAbstractDeclarator = ctx.directAbstractDeclarator();
        return parseFromDirectAbstractDeclarator(baseType, directAbstractDeclarator);
    }

    private TypeNode parseFromDirectAbstractDeclarator(
        TypeNode baseType,
        C99Parser.DirectAbstractDeclaratorContext ctx) {
        // directAbstractDeclarator -> LeftParen abstractDeclarator RightParen
        if (ctx.abstractDeclarator() != null) {
            return parseFromAbstractDeclarator(baseType, ctx.abstractDeclarator());
        }

        // directAbstractDeclarator -> directAbstractDeclarator LeftParen parameterTypeList? RightParen
        if (ctx.directAbstractDeclarator() != null) {
            baseType = parseFromDirectAbstractDeclarator(baseType, ctx.directAbstractDeclarator());
        }

        if (ctx.LeftParen() != null) {
            // directAbstractDeclarator -> directAbstractDeclarator LeftParen parameterTypeList? RightParen
            // directAbstractDeclarator -> LeftParen parameterTypeList? RightParen

            // 构造函数类型
            if (ctx.parameterTypeList() != null) {
                return parseFromParameterTypeList(baseType, ctx.parameterTypeList(), ctx.RightParen());
            } else {
                // 无参数函数类型
                return new FunctionTypeNode(
                    SourceLocation.concat(baseType.getWholeLocation(), getSourceLocation(ctx.RightParen())),
                    baseType, new ArrayList<>(), new ArrayList<>());
            }
        }

        throw new RuntimeException("Unknown direct declarator: " + ctx.getText());
    }

    private FunctionTypeNode parseFromParameterTypeList(
        TypeNode returnType, C99Parser.ParameterTypeListContext ctx, TerminalNode rightParen) {
        // 构造函数类型
        List<TypeNode> parameterTypes = new ArrayList<>();
        List<IdentifierNode> parameters = new ArrayList<>();

        // parameterTypeList
        // -> parameterList
        // -> parameterDeclaration (Comma parameterDeclaration)*
        var paramDeclList = ctx.parameterList().parameterDeclaration();
        for (C99Parser.ParameterDeclarationContext paramCtx : paramDeclList) {
            // parameterDeclaration -> declarationSpecifiers declarator?
            // TypeNode paramBaseType = parseDeclarationSpecifiers(paramCtx.declarationSpecifiers());
            TypeAndSpecifiers paramTypeAndSpecifiers = parseDeclarationSpecifiers(paramCtx.declarationSpecifiers());
            TypeNode paramBaseType = paramTypeAndSpecifiers.t;
            StorageClassSpecifierNode paramStorageClass = paramTypeAndSpecifiers.storageClass;

            if (paramCtx.declarator() == null) {
                // 没有参数名
                if (paramBaseType == null) {
                    // 没有类型
                    error();
                    String msg = "type defaults to '" + logger.white("int") + "' in type name";
                    assert paramStorageClass != null; // 没有类型，那么一定有存储类型
                    logErrorWithSourceLine(paramStorageClass.wholeLoc, msg);
                    paramBaseType = new BasicTypeNode(null, BasicType.INT);
                }
                if (paramStorageClass != null && paramStorageClass.storageClass != StorageClassSpecifier.REGISTER) {
                    // 有非 register 的存储类型
                    error();
                    String msg = "storage class specified for unnamed parameter";
                    logErrorWithSourceLine(paramStorageClass.wholeLoc, msg);
                }
                parameterTypes.add(paramBaseType);
                parameters.add(null);
            } else {
                // 具名参数
                boolean baseTypeError = paramBaseType == null;
                if (paramBaseType == null) {
                    // 没有类型
                    paramBaseType = new BasicTypeNode(null, BasicType.INT);
                }
                DeclarationLikeResult paramRes = parseFromDeclarator(paramBaseType, paramCtx.declarator());
                if (baseTypeError) {
                    error();
                    String msg = "type defaults to '" + logger.white("int") + "' in declaration of '" +
                                 logger.white(paramRes.id.id) + "'";
                    logErrorWithSourceLine(paramRes.id.wholeLoc, msg);
                }
                if (paramStorageClass != null && paramStorageClass.storageClass != StorageClassSpecifier.REGISTER) {
                    // 有非 register 的存储类型
                    error();
                    String msg = "storage class specified for parameter '" + logger.white(paramRes.id.id) + "'";
                    logErrorWithSourceLine(paramStorageClass.wholeLoc, msg);
                }

                parameterTypes.add(paramRes.t);
                parameters.add(paramRes.id);
            }
        }
        return new FunctionTypeNode(
            SourceLocation.concat(returnType.getWholeLocation(), getSourceLocation(rightParen)),
            returnType, parameterTypes, parameters);

    }

    @Override
    public DeclarationNode visitDeclaration(C99Parser.DeclarationContext ctx) {
        // declaration -> declarationSpecifiers initDeclaratorList? Semicolon
        var list = ctx.initDeclaratorList();
        if (list != null) {
            // initDeclaratorList
            // -> initDeclarator
            // -> declarator (Assign initializer)?
            var initDeclarator = list.initDeclarator();
            TypeAndSpecifiers typeAndSpecifiers = parseDeclarationSpecifiers(ctx.declarationSpecifiers());
            TypeNode baseType = typeAndSpecifiers.t;
            boolean isBaseTypeError = baseType == null;
            if (isBaseTypeError) {
                // 没有类型
                baseType = new BasicTypeNode(null, BasicType.INT);
            }
            DeclarationLikeResult res = parseFromDeclarator(baseType, initDeclarator.declarator());

            if (isBaseTypeError) {
                error();
                String msg =
                    "type defaults to '" + logger.white("int") + "' in declaration of '" + logger.white(res.id.id) +
                    "'";
                logErrorWithSourceLine(res.id.wholeLoc, msg);
            }

            IdentifierNode id = res.id;
            TypeNode t = res.t;
            if (initDeclarator.initializer() != null) {
                ExpressionNode init = (ExpressionNode) visit(initDeclarator.initializer());
                return new DeclarationNode(getSourceLocation(ctx), typeAndSpecifiers.storageClass, t, id, init);
            } else {
                return new DeclarationNode(getSourceLocation(ctx), typeAndSpecifiers.storageClass, t, id);
            }
        } else {
            return null;
        }
    }

    @Override
    public StatementNode visitLabeledStatement(C99Parser.LabeledStatementContext ctx) {
        StatementNode statement = (StatementNode) visit(ctx.statement());
        if (ctx.Identifier() != null) {
            String label = ctx.Identifier().getText();
            SourceLocation labelLocation = getSourceLocation(ctx.Identifier());
            statement.gotoLabels.add(new StatementNode.GotoLabelInfo(new IdentifierNode(labelLocation, label)));
            return statement;
        } else if (ctx.Case() != null) {
            SourceLocation caseLocation = getSourceLocation(ctx.Case());
            ExpressionNode caseValue = parseIntegerConstant(ctx.IntegerConstant());
            statement.caseLabels.add(new StatementNode.CaseLabelInfo(caseLocation, caseValue));
            return statement;
        } else if (ctx.Default() != null) {
            SourceLocation defaultLocation = getSourceLocation(ctx.Default());
            statement.defaultLabels.add(new StatementNode.DefaultLabelInfo(defaultLocation));
            return statement;
        } else {
            throw new IllegalStateException("Unknown labeled statement");
        }
    }

    @Override
    public CompoundStatementNode visitCompoundStatement(C99Parser.CompoundStatementContext ctx) {
        List<BlockItemNode> blockItems = new ArrayList<>();
        if (ctx.blockItemList() != null) {
            for (C99Parser.BlockItemContext blockItemCtx : ctx.blockItemList().blockItem()) {
                AstNode blockItem = visit(blockItemCtx);
                if (blockItem instanceof DeclarationNode) {
                    blockItems.add(new DeclarationBlockItemNode((DeclarationNode) blockItem));
                } else if (blockItem instanceof StatementNode) {
                    blockItems.add(new StatementBlockItemNode((StatementNode) blockItem));
                } else {
                    throw new IllegalStateException("Unknown block item");
                }
            }
        }
        return new CompoundStatementNode(getSourceLocation(ctx), blockItems);
    }

    @Override
    public StatementNode visitExpressionStatement(C99Parser.ExpressionStatementContext ctx) {
        if (ctx.expression() != null) {
            return new ExpressionStatementNode((ExpressionNode) visit(ctx.expression()));
        } else {
            return new NullStatementNode(getSourceLocation(ctx.Semicolon()));
        }
    }

    @Override
    public StatementNode visitSelectionStatement(C99Parser.SelectionStatementContext ctx) {
        if (ctx.If() != null) {
            ExpressionNode cond = (ExpressionNode) visit(ctx.expression());
            StatementNode thenStmt = (StatementNode) visit(ctx.statement(0));
            if (ctx.Else() != null) {
                StatementNode elseStmt = (StatementNode) visit(ctx.statement(1));
                return new IfStatementNode(cond, thenStmt, elseStmt);
            } else {
                return new IfStatementNode(cond, thenStmt);
            }
        } else if (ctx.Switch() != null) {
            ExpressionNode exp = (ExpressionNode) visit(ctx.expression());
            StatementNode body = (StatementNode) visit(ctx.statement(0));
            return new SwitchStatementNode(exp, body);
        } else {
            throw new IllegalStateException("Unknown selection statement");
        }
    }

    @Override
    public StatementNode visitIterationStatement(C99Parser.IterationStatementContext ctx) {
        if (ctx.Do() != null) {
            StatementNode body = (StatementNode) visit(ctx.statement());
            ExpressionNode cond = (ExpressionNode) visit(ctx.expression(0));
            return new WhileLoopNode(getSourceLocation(ctx), cond, body, true);
        } else if (ctx.While() != null) {
            ExpressionNode cond = (ExpressionNode) visit(ctx.expression(0));
            StatementNode body = (StatementNode) visit(ctx.statement());
            return new WhileLoopNode(getSourceLocation(ctx), cond, body);
        } else if (ctx.For() != null) {
            ForInitNode init = null;
            ExpressionNode cond = null;
            ExpressionNode step = null;
            StatementNode body = (StatementNode) visit(ctx.statement());
            if (ctx.declaration() != null) {
                DeclarationNode decl = visitDeclaration(ctx.declaration());
                if (decl != null) {
                    init = new ForInitDeclarationNode(decl);
                }
            } else {
                if (ctx.init != null) {
                    init = new ForInitExpressionNode((ExpressionNode) visit(ctx.init));
                }
            }
            if (ctx.cond != null) {
                cond = (ExpressionNode) visit(ctx.cond);
            }
            if (ctx.step != null) {
                step = (ExpressionNode) visit(ctx.step);
            }
            return new ForLoopNode(getSourceLocation(ctx), init, cond, step, body);
        } else {
            throw new IllegalStateException("Unknown iteration statement");
        }
    }

    @Override
    public StatementNode visitJumpStatement(C99Parser.JumpStatementContext ctx) {
        if (ctx.Goto() != null) {
            String identifier = ctx.Identifier().getText();
            SourceLocation identifierLocation = getSourceLocation(ctx.Identifier());
            return new GotoNode(getSourceLocation(ctx.Goto()), new IdentifierNode(identifierLocation, identifier));
        } else if (ctx.Continue() != null) {
            return new ContinueNode(getSourceLocation(ctx.Continue()));
        } else if (ctx.Break() != null) {
            return new BreakNode(getSourceLocation(ctx.Break()));
        } else if (ctx.Return() != null) {
            ExpressionNode expression = (ExpressionNode) visit(ctx.expression());
            return new ReturnNode(getSourceLocation(ctx.Return()), expression);
        } else {
            throw new IllegalStateException("Unknown jump statement");
        }
    }

    private ExpressionNode parseIntegerConstant(TerminalNode integerConstant) {
        String fullText = integerConstant.getText();
        int pos = fullText.length();

        while (pos > 0) {
            char c = fullText.charAt(pos - 1);
            if (c == 'l' || c == 'L') {
                pos--;
            } else {
                break;
            }
        }
        String suffix = fullText.substring(pos).toLowerCase(); // l, ll
        String numberPart = fullText.substring(0, pos);

        int radix;
        if (numberPart.startsWith("0x") || numberPart.startsWith("0X")) {
            radix = 16;
            numberPart = numberPart.substring(2);
        } else if (numberPart.startsWith("0") && numberPart.length() > 1) {
            radix = 8;
        } else {
            radix = 10;
        }

        BigInteger bigValue = new BigInteger(numberPart, radix);
        SourceLocation loc = getSourceLocation(integerConstant);

        // 必须能容纳在 64 位有符号整数中
        if (bigValue.bitLength() > 63) {
            error();
            String msg = "integer constant is too large for its type";
            logErrorWithSourceLine(loc, msg);
            return new ConstantNode(loc, 0L);
        }

        boolean isLong = !suffix.isEmpty();
        if (isLong) {
            return new ConstantNode(loc, bigValue.longValue());
        } else {
            // 无后缀，先尝试 int
            if (bigValue.bitLength() < 32) {
                return new ConstantNode(loc, bigValue.intValue());
            } else {
                // 超出 int，放入 long
                return new ConstantNode(loc, bigValue.longValue());
            }
        }
    }

    @Override
    public ExpressionNode visitPrimaryExpression(C99Parser.PrimaryExpressionContext ctx) {
        if (ctx.Identifier() != null) {
            String identifier = ctx.Identifier().getText();
            return new IdentifierNode(getSourceLocation(ctx.Identifier()), identifier);
        } else if (ctx.IntegerConstant() != null) {
            return parseIntegerConstant(ctx.IntegerConstant());
        } else if (ctx.LeftParen() != null) {
            return (ExpressionNode) visit(ctx.expression());
        } else {
            throw new IllegalStateException("Unknown primary expression");
        }
    }

    @Override
    public ExpressionNode visitPostfixExpression(C99Parser.PostfixExpressionContext ctx) {
        if (ctx.primaryExpression() != null) {
            return (ExpressionNode) visit(ctx.primaryExpression());
        }

        if (ctx.PlusPlus() != null) {
            ExpressionNode operand = (ExpressionNode) visit(ctx.postfixExpression());
            return new IncrementDecrementNode(getSourceLocation(ctx.PlusPlus()), true, false, operand);
        }

        if (ctx.MinusMinus() != null) {
            ExpressionNode operand = (ExpressionNode) visit(ctx.postfixExpression());
            return new IncrementDecrementNode(getSourceLocation(ctx.MinusMinus()), false, false, operand);
        }

        if (ctx.LeftParen() != null) {
            ExpressionNode function = (ExpressionNode) visit(ctx.postfixExpression());
            List<ExpressionNode> arguments = new ArrayList<>();
            if (ctx.argumentExpressionList() != null) {
                var argExpList = ctx.argumentExpressionList();
                for (C99Parser.AssignmentExpressionContext argCtx : argExpList.assignmentExpression()) {
                    arguments.add((ExpressionNode) visit(argCtx));
                }
            }
            return new FunctionCallNode(getSourceLocation(ctx), function, arguments);
        }

        throw new IllegalStateException("Unknown postfix expression");
    }

    @Override
    public ExpressionNode visitUnaryExpression(C99Parser.UnaryExpressionContext ctx) {
        if (ctx.postfixExpression() != null) {
            return (ExpressionNode) visit(ctx.postfixExpression());
        } else if (ctx.PlusPlus() != null) {
            ExpressionNode operand = (ExpressionNode) visit(ctx.unaryExpression());
            return new IncrementDecrementNode(getSourceLocation(ctx.PlusPlus()), true, true, operand);
        } else if (ctx.MinusMinus() != null) {
            ExpressionNode operand = (ExpressionNode) visit(ctx.unaryExpression());
            return new IncrementDecrementNode(getSourceLocation(ctx.MinusMinus()), false, true, operand);
        } else if (ctx.unaryOperator() != null) {
            UnaryOperatorNode operator = (UnaryOperatorNode) visit(ctx.unaryOperator());
            ExpressionNode operand = (ExpressionNode) visit(ctx.castExpression());
            return new UnaryExpressionNode(operator, operand);
        } else {
            throw new IllegalStateException("Unknown unary operator");
        }
    }

    @Override
    public UnaryOperatorNode visitUnaryOperator(C99Parser.UnaryOperatorContext ctx) {
        String operator = ctx.getText();
        UnaryOperator op = UnaryOperator.fromSymbol(operator);
        return new UnaryOperatorNode(getSourceLocation(ctx), op);
    }

    private TypeNode parseFromTypeName(C99Parser.TypeNameContext ctx) {
        TypeCombinationHelper helper = new TypeCombinationHelper();

        for (var specifierQualifierCtx : ctx.specifierQualifierList().specifierQualifier()) {
            if (specifierQualifierCtx.typeSpecifier() != null) {
                String typeSpecifierText = specifierQualifierCtx.typeSpecifier().getText();
                helper.append(typeSpecifierText, getSourceLocation(specifierQualifierCtx.typeSpecifier()));
            }
        }

        TypeNode type = new BasicTypeNode(helper.loc, helper.t);
        if (ctx.abstractDeclarator() != null) {
            type = parseFromAbstractDeclarator(type, ctx.abstractDeclarator());
        }
        return type;
    }

    @Override
    public ExpressionNode visitCastExpression(C99Parser.CastExpressionContext ctx) {
        if (ctx.unaryExpression() != null) {
            return (ExpressionNode) visit(ctx.unaryExpression());
        } else {
            TypeNode type = parseFromTypeName(ctx.typeName());
            ExpressionNode operand = (ExpressionNode) visit(ctx.castExpression());
            return new CastExpressionNode(getSourceLocation(ctx), type, operand);
        }
    }

    @Override
    public ExpressionNode visitMultiplicativeExpression(C99Parser.MultiplicativeExpressionContext ctx) {
        if (ctx.multiplicativeOperator() != null) {
            BinaryOperatorNode op = visitMultiplicativeOperator(ctx.multiplicativeOperator());
            ExpressionNode lhs = (ExpressionNode) visit(ctx.multiplicativeExpression());
            ExpressionNode rhs = (ExpressionNode) visit(ctx.castExpression());
            return new BinaryExpressionNode(op, lhs, rhs);
        } else if (ctx.castExpression() != null) {
            return (ExpressionNode) visit(ctx.castExpression());
        } else {
            throw new IllegalStateException("Unknown multiplicative expression");
        }
    }

    @Override
    public BinaryOperatorNode visitMultiplicativeOperator(C99Parser.MultiplicativeOperatorContext ctx) {
        String operator = ctx.getText();
        BinaryOperator op = BinaryOperator.fromSymbol(operator);
        return new BinaryOperatorNode(getSourceLocation(ctx), op);
    }

    @Override
    public ExpressionNode visitAdditiveExpression(C99Parser.AdditiveExpressionContext ctx) {
        if (ctx.additiveOperator() != null) {
            BinaryOperatorNode op = visitAdditiveOperator(ctx.additiveOperator());
            ExpressionNode lhs = (ExpressionNode) visit(ctx.additiveExpression());
            ExpressionNode rhs = (ExpressionNode) visit(ctx.multiplicativeExpression());
            return new BinaryExpressionNode(op, lhs, rhs);
        } else if (ctx.multiplicativeExpression() != null) {
            return (ExpressionNode) visit(ctx.multiplicativeExpression());
        } else {
            throw new IllegalStateException("Unknown additive expression");
        }
    }

    @Override
    public BinaryOperatorNode visitAdditiveOperator(C99Parser.AdditiveOperatorContext ctx) {
        String operator = ctx.getText();
        BinaryOperator op = BinaryOperator.fromSymbol(operator);
        return new BinaryOperatorNode(getSourceLocation(ctx), op);
    }

    @Override
    public ExpressionNode visitShiftExpression(C99Parser.ShiftExpressionContext ctx) {
        if (ctx.shiftOperator() != null) {
            BinaryOperatorNode op = visitShiftOperator(ctx.shiftOperator());
            ExpressionNode lhs = (ExpressionNode) visit(ctx.shiftExpression());
            ExpressionNode rhs = (ExpressionNode) visit(ctx.additiveExpression());
            return new BinaryExpressionNode(op, lhs, rhs);
        } else if (ctx.additiveExpression() != null) {
            return (ExpressionNode) visit(ctx.additiveExpression());
        } else {
            throw new IllegalStateException("Unknown shift expression");
        }
    }

    @Override
    public BinaryOperatorNode visitShiftOperator(C99Parser.ShiftOperatorContext ctx) {
        String operator = ctx.getText();
        BinaryOperator op = BinaryOperator.fromSymbol(operator);
        return new BinaryOperatorNode(getSourceLocation(ctx), op);
    }

    @Override
    public ExpressionNode visitRelationalExpression(C99Parser.RelationalExpressionContext ctx) {
        if (ctx.relationalOperator() != null) {
            BinaryOperatorNode op = visitRelationalOperator(ctx.relationalOperator());
            ExpressionNode lhs = (ExpressionNode) visit(ctx.relationalExpression());
            ExpressionNode rhs = (ExpressionNode) visit(ctx.shiftExpression());
            return new BinaryExpressionNode(op, lhs, rhs);
        } else if (ctx.shiftExpression() != null) {
            return (ExpressionNode) visit(ctx.shiftExpression());
        } else {
            throw new IllegalStateException("Unknown relational expression");
        }
    }

    @Override
    public BinaryOperatorNode visitRelationalOperator(C99Parser.RelationalOperatorContext ctx) {
        String operator = ctx.getText();
        BinaryOperator op = BinaryOperator.fromSymbol(operator);
        return new BinaryOperatorNode(getSourceLocation(ctx), op);
    }

    @Override
    public ExpressionNode visitEqualityExpression(C99Parser.EqualityExpressionContext ctx) {
        if (ctx.equalityOperator() != null) {
            BinaryOperatorNode op = visitEqualityOperator(ctx.equalityOperator());
            ExpressionNode lhs = (ExpressionNode) visit(ctx.equalityExpression());
            ExpressionNode rhs = (ExpressionNode) visit(ctx.relationalExpression());
            return new BinaryExpressionNode(op, lhs, rhs);
        } else if (ctx.relationalExpression() != null) {
            return (ExpressionNode) visit(ctx.relationalExpression());
        } else {
            throw new IllegalStateException("Unknown equality expression");
        }
    }

    @Override
    public BinaryOperatorNode visitEqualityOperator(C99Parser.EqualityOperatorContext ctx) {
        String operator = ctx.getText();
        BinaryOperator op = BinaryOperator.fromSymbol(operator);
        return new BinaryOperatorNode(getSourceLocation(ctx), op);
    }

    @Override
    public ExpressionNode visitAndExpression(C99Parser.AndExpressionContext ctx) {
        if (ctx.And() != null) {
            BinaryOperatorNode op = new BinaryOperatorNode(getSourceLocation(ctx.And()), BinaryOperator.BITWISE_AND);
            ExpressionNode lhs = (ExpressionNode) visit(ctx.andExpression());
            ExpressionNode rhs = (ExpressionNode) visit(ctx.equalityExpression());
            return new BinaryExpressionNode(op, lhs, rhs);
        } else if (ctx.equalityExpression() != null) {
            return (ExpressionNode) visit(ctx.equalityExpression());
        } else {
            throw new IllegalStateException("Unknown and expression");
        }
    }

    @Override
    public ExpressionNode visitExclusiveOrExpression(C99Parser.ExclusiveOrExpressionContext ctx) {
        if (ctx.Caret() != null) {
            BinaryOperatorNode op = new BinaryOperatorNode(getSourceLocation(ctx.Caret()), BinaryOperator.BITWISE_XOR);
            ExpressionNode lhs = (ExpressionNode) visit(ctx.exclusiveOrExpression());
            ExpressionNode rhs = (ExpressionNode) visit(ctx.andExpression());
            return new BinaryExpressionNode(op, lhs, rhs);
        } else if (ctx.andExpression() != null) {
            return (ExpressionNode) visit(ctx.andExpression());
        } else {
            throw new IllegalStateException("Unknown exclusive or expression");
        }
    }

    @Override
    public ExpressionNode visitInclusiveOrExpression(C99Parser.InclusiveOrExpressionContext ctx) {
        if (ctx.Or() != null) {
            BinaryOperatorNode op = new BinaryOperatorNode(getSourceLocation(ctx.Or()), BinaryOperator.BITWISE_OR);
            ExpressionNode lhs = (ExpressionNode) visit(ctx.inclusiveOrExpression());
            ExpressionNode rhs = (ExpressionNode) visit(ctx.exclusiveOrExpression());
            return new BinaryExpressionNode(op, lhs, rhs);
        } else if (ctx.exclusiveOrExpression() != null) {
            return (ExpressionNode) visit(ctx.exclusiveOrExpression());
        } else {
            throw new IllegalStateException("Unknown inclusive or expression");
        }
    }

    @Override
    public ExpressionNode visitLogicalAndExpression(C99Parser.LogicalAndExpressionContext ctx) {
        if (ctx.AndAnd() != null) {
            BinaryOperatorNode op = new BinaryOperatorNode(getSourceLocation(ctx.AndAnd()), BinaryOperator.LOGICAL_AND);
            ExpressionNode lhs = (ExpressionNode) visit(ctx.logicalAndExpression());
            ExpressionNode rhs = (ExpressionNode) visit(ctx.inclusiveOrExpression());
            return new BinaryExpressionNode(op, lhs, rhs);
        } else if (ctx.inclusiveOrExpression() != null) {
            return (ExpressionNode) visit(ctx.inclusiveOrExpression());
        } else {
            throw new IllegalStateException("Unknown logical and expression");
        }
    }

    @Override
    public ExpressionNode visitLogicalOrExpression(C99Parser.LogicalOrExpressionContext ctx) {
        if (ctx.OrOr() != null) {
            BinaryOperatorNode op = new BinaryOperatorNode(getSourceLocation(ctx.OrOr()), BinaryOperator.LOGICAL_OR);
            ExpressionNode lhs = (ExpressionNode) visit(ctx.logicalOrExpression());
            ExpressionNode rhs = (ExpressionNode) visit(ctx.logicalAndExpression());
            return new BinaryExpressionNode(op, lhs, rhs);
        } else if (ctx.logicalAndExpression() != null) {
            return (ExpressionNode) visit(ctx.logicalAndExpression());
        } else {
            throw new IllegalStateException("Unknown logical or expression");
        }
    }

    @Override
    public ExpressionNode visitConditionalExpression(C99Parser.ConditionalExpressionContext ctx) {
        if (ctx.Question() != null) {
            ExpressionNode cond = (ExpressionNode) visit(ctx.logicalOrExpression());
            ExpressionNode thenExpr = (ExpressionNode) visit(ctx.expression());
            ExpressionNode elseExpr = (ExpressionNode) visit(ctx.conditionalExpression());
            return new ConditionalExpressionNode(cond, thenExpr, elseExpr);
        } else if (ctx.logicalOrExpression() != null) {
            return (ExpressionNode) visit(ctx.logicalOrExpression());
        } else {
            throw new IllegalStateException("Unknown conditional expression");
        }
    }

    @Override
    public ExpressionNode visitAssignmentExpression(C99Parser.AssignmentExpressionContext ctx) {
        if (ctx.conditionalExpression() != null) {
            return (ExpressionNode) visit(ctx.conditionalExpression());
        } else if (ctx.assignmentOperator() != null) {
            AssignmentOperatorNode op = visitAssignmentOperator(ctx.assignmentOperator());
            ExpressionNode lhs = (ExpressionNode) visit(ctx.unaryExpression());
            ExpressionNode rhs = (ExpressionNode) visit(ctx.assignmentExpression());
            return new AssignmentNode(op, lhs, rhs);
        } else {
            throw new IllegalStateException("Unknown assignment expression");
        }
    }

    @Override
    public AssignmentOperatorNode visitAssignmentOperator(C99Parser.AssignmentOperatorContext ctx) {
        String operator = ctx.getText();
        AssignmentOperator op = AssignmentOperator.fromSymbol(operator);
        return new AssignmentOperatorNode(getSourceLocation(ctx), op);
    }
}
