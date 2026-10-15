package net.flymachine.minecraftclanguage.content.logic.compiler.frontend.c99;

import net.flymachine.minecraftclanguage.content.logic.compiler.frontend.antlr.C99Lexer;
import net.flymachine.minecraftclanguage.content.logic.compiler.frontend.antlr.C99Parser;
import org.antlr.v4.runtime.Parser;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.TokenStream;
import org.antlr.v4.runtime.tree.TerminalNode;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;

public abstract class C99ParserBase extends Parser {

    /**
     * typedef 名作用域栈。栈顶是当前作用域
     * <p>
     * 值为 true 表示该名字是 typedef 名
     * <p>
     * false 表示被同作用域的非 typedef 声明遮蔽
     * <p>
     * 查找由内向外，命中第一个含该名字的作用域即返回
     */
    private final Stack<Map<String, Boolean>> typedefScopes = new Stack<>();

    protected C99ParserBase(TokenStream input) {
        super(input);
        typedefScopes.push(new HashMap<>());
    }

    /**
     * 供语法里的 {@code {isTypedefName()}?} 调用。
     * 先检查作用域，再检查当前说明符列表是否已有完整类型说明符。
     */
    public boolean isTypedefName() {
        return isTypedefName(1);
    }

    /**
     * 纯作用域查询：判断指定偏移处的 token 是否为当前可见的 typedef 名。
     * 供 {@link #isTypeName()} 使用，不受上下文列表影响。
     */
    public boolean isTypedefName(int k) {
        if (_input.LA(k) != C99Lexer.Identifier) {
            return false;
        }
        String text = _input.LT(k).getText();
        for (int i = typedefScopes.size() - 1; i >= 0; i--) {
            Boolean v = typedefScopes.get(i).get(text);
            if (v != null) {
                return v;
            }
        }
        return false;
    }

    /**
     * 判断当前是否应继续接受新的类型说明符。
     * <p>
     * {@code int T;} 里 {@code int} 已是完整类型，{@code T} 不能再作为说明符；
     * {@code signed T x;} 里 {@code signed} 不完整，{@code T} 可以是 typedefName。
     * <p>
     * 谓词在 {@code declarationSpecifier} 或 {@code specifierQualifier} 规则的开头执行，
     * 此时 {@code getContext()} 是各自的 context，其 parent 是列表 context。
     */
    public boolean isDeclarationSpecifier() {
        // 关键字和 const 直接放行，不做列表完整性检查
        if (_input.LA(1) != C99Lexer.Identifier) {
            return true;
        }
        ParserRuleContext ctx = getContext();
        while (ctx != null) {
            if (ctx instanceof C99Parser.DeclarationSpecifiersContext dsCtx) {
                return !hasCompleteInList(dsCtx.declarationSpecifier());
            }
            if (ctx instanceof C99Parser.SpecifierQualifierListContext sqCtx) {
                for (C99Parser.SpecifierQualifierContext spec : sqCtx.specifierQualifier()) {
                    C99Parser.TypeSpecifierContext ts = spec.typeSpecifier();
                    if (ts != null && isCompleteTypeSpecifier(ts)) {
                        return false;
                    }
                }
                return true;
            }
            if (ctx instanceof C99Parser.DeclaratorContext
                || ctx instanceof C99Parser.ParameterDeclarationContext
                || ctx instanceof C99Parser.StructDeclarationContext
                || ctx instanceof C99Parser.StructOrUnionSpecifierContext
                || ctx instanceof C99Parser.TypeNameContext
                || ctx instanceof C99Parser.FunctionDefinitionContext
                || ctx instanceof C99Parser.DeclarationContext) {
                break;
            }
            ctx = ctx.getParent();
        }
        return true;
    }

    private boolean hasCompleteInList(List<C99Parser.DeclarationSpecifierContext> specs) {
        for (C99Parser.DeclarationSpecifierContext spec : specs) {
            C99Parser.TypeSpecifierContext ts = spec.typeSpecifier();
            if (ts != null && isCompleteTypeSpecifier(ts)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 判断单个 typeSpecifier 是否为完整类型说明符
     * <p>
     * {@code void}、{@code char}、{@code int}、{@code double}、struct/union/enum、
     * typedefName 都是完整的；{@code long}、{@code signed}、{@code unsigned} 不是。
     */
    private boolean isCompleteTypeSpecifier(C99Parser.TypeSpecifierContext ts) {
        if (ts.Void() != null || ts.Char() != null
            || ts.Int() != null || ts.Double() != null) {
            return true;
        }
        if (ts.Long() != null || ts.Signed() != null || ts.Unsigned() != null) {
            return false;
        }
        if (ts.structOrUnionSpecifier() != null || ts.enumSpecifier() != null) {
            return true;
        }
        if (ts.typedefName() != null) {
            return true;
        }
        return false;
    }

    public boolean isTypeName() {
        int i = 1;
        if (_input.LA(i) == C99Lexer.Sizeof) {
            i++;
        }
        if (_input.LA(i) != C99Lexer.LeftParen) {
            return false;
        }
        i++;
        int t = _input.LA(i);
        if (isTypeKeyword(t)) {
            return true;
        }
        if (t == C99Lexer.Identifier) {
            return isTypedefName(i);
        }
        return false;
    }

    private static boolean isTypeKeyword(int t) {
        return t == C99Lexer.Void || t == C99Lexer.Char || t == C99Lexer.Int
               || t == C99Lexer.Long || t == C99Lexer.Double
               || t == C99Lexer.Signed || t == C99Lexer.Unsigned
               || t == C99Lexer.Struct || t == C99Lexer.Union || t == C99Lexer.Enum
               || t == C99Lexer.Const;
    }

    /**
     * 当前声明是否为 typedef，由 declarationSpecifiers 匹配后设置。
     */
    private boolean currentDeclarationIsTypedef = false;

    public void setCurrentDeclarationTypedef(C99Parser.DeclarationSpecifiersContext specifiers) {
        this.currentDeclarationIsTypedef = hasTypedefKeyword(specifiers);
    }

    /**
     * {@code initDeclarator} 规则末尾调用，注册当前声明符的名字。
     * <p>
     * 每个声明符匹配完成后立即注册，使后续声明符的初始化式能引用它。
     * 例如 {@code typedef int T, *pT[sizeof(T)];} 中
     * 第二个声明符的 {@code sizeof(T)} 应能解析到刚声明的 {@code T}。
     */
    public void registerInitDeclarator(C99Parser.DeclaratorContext declarator) {
        String name = extractDeclaratorName(declarator);
        if (name == null) {
            return;
        }
        boolean isTypedef = currentDeclarationIsTypedef && !insideForDeclaration();
        typedefScopes.peek().put(name, isTypedef);
    }

    /**
     * 判断当前 declarator 是否位于 for 语句的声明部分
     * <p>
     * C99 6.8.5.3p1：for 的声明部分只能声明 auto 或 register 存储类的对象，
     * 因此其中不允许出现 typedef。Parser 阶段按非 typedef 处理，
     * 约束错误留给语义分析。
     * <p>
     * 向上遍历遇到 CompoundStatement 或 FunctionDefinition 即停止，
     * 避免误判 for 体内嵌套块里的普通声明。
     */
    private boolean insideForDeclaration() {
        ParserRuleContext p = getContext();
        while (p != null) {
            if (p instanceof C99Parser.CompoundStatementContext) {
                return false;
            }
            if (p instanceof C99Parser.FunctionDefinitionContext) {
                return false;
            }
            if (p instanceof C99Parser.IterationStatementContext iter) {
                return iter.For() != null;
            }
            p = p.getParent();
        }
        return false;
    }

    /**
     * {@code parameterDeclaration} 规则末尾调用，注册参数名。
     * 参数一定是非 typedef 名，会遮蔽外层同名 typedef。
     */
    public void registerParameter(C99Parser.DeclaratorContext declarator) {
        String name = extractDeclaratorName(declarator);
        if (name != null) {
            typedefScopes.peek().put(name, false);
        }
    }

    /**
     * {@code enumerator} 规则末尾调用，注册枚举常量名。
     */
    public void registerEnumerator(TerminalNode identifier) {
        typedefScopes.peek().put(identifier.getText(), false);
    }

    private boolean hasTypedefKeyword(C99Parser.DeclarationSpecifiersContext specifiers) {
        if (specifiers == null) {
            return false;
        }
        // 一个声明只允许一个存储类说明符；多余的是错误
        // 但解析阶段宽容处理，只让第一个生效，与 AST 层保持一致
        for (C99Parser.DeclarationSpecifierContext spec : specifiers.declarationSpecifier()) {
            C99Parser.StorageClassSpecifierContext sc = spec.storageClassSpecifier();
            if (sc != null) {
                return sc.Typedef() != null;
            }
        }
        return false;
    }

    /**
     * {@code functionDefinition} 中，{@code declarator} 之前调用。
     * 建立函数作用域，覆盖参数和函数体。
     */
    public void enterFunctionScope() {
        typedefScopes.push(new HashMap<>());
    }

    /**
     * {@code functionDefinition} 中，{@code declarator} 之后调用。
     * 函数名注册到外层作用域（临时弹出函数作用域再压回）。
     */
    public void registerFunctionName(C99Parser.DeclaratorContext declarator) {
        String name = extractDeclaratorName(declarator);
        if (name == null) {
            return;
        }
        Map<String, Boolean> funcScope = typedefScopes.pop();
        try {
            typedefScopes.peek().put(name, false);
        } finally {
            typedefScopes.push(funcScope);
        }
    }

    public void exitFunctionScope() {
        typedefScopes.pop();
    }

    /**
     * {@code compoundStatement} 中，{@code LeftBrace} 之后调用。
     * 若当前块是函数定义的直接函数体，函数作用域已建立，跳过。
     */
    public void enterBlockScope() {
        if (getContext().getParent() instanceof C99Parser.FunctionDefinitionContext) {
            return;
        }
        typedefScopes.push(new HashMap<>());
    }

    public void exitBlockScope() {
        if (getContext().getParent() instanceof C99Parser.FunctionDefinitionContext) {
            return;
        }
        typedefScopes.pop();
    }

    /**
     * {@code directDeclarator} 的 {@code ( parameterTypeList )} 分支，
     * {@code LeftParen} 之后调用。
     * <p>
     * 函数定义的参数列表由函数作用域覆盖，不另建作用域；
     * 函数声明的参数列表建立独立的函数原型作用域。
     */
    public void enterParameterScope() {
        if (isFunctionDefinitionParameterList()) {
            return;
        }
        typedefScopes.push(new HashMap<>());
    }

    public void exitParameterScope() {
        if (isFunctionDefinitionParameterList()) {
            return;
        }
        typedefScopes.pop();
    }

    /**
     * 判断当前参数列表是不是函数定义的参数列表。
     * <p>
     * 条件：DirectDeclaratorContext 的直接父是 DeclaratorContext，
     * 且该 DeclaratorContext 的直接父是 FunctionDefinitionContext。
     * <p>
     * 嵌套的参数列表（例如函数指针参数的参数列表）不满足此条件，
     * 按函数原型作用域处理。
     */
    private boolean isFunctionDefinitionParameterList() {
        ParserRuleContext p = getContext();
        if (!(p instanceof C99Parser.DirectDeclaratorContext)) {
            return false;
        }
        if (!(p.getParent() instanceof C99Parser.DeclaratorContext)) {
            return false;
        }
        return p.getParent().getParent() instanceof C99Parser.FunctionDefinitionContext;
    }

    private String extractDeclaratorName(C99Parser.DeclaratorContext ctx) {
        if (ctx == null) {
            return null;
        }
        return extractFromDirect(ctx.directDeclarator());
    }

    private String extractFromDirect(C99Parser.DirectDeclaratorContext ctx) {
        if (ctx == null) {
            return null;
        }
        if (ctx.Identifier() != null) {
            return ctx.Identifier().getText();
        }
        if (ctx.declarator() != null) {
            return extractDeclaratorName(ctx.declarator());
        }
        if (ctx.directDeclarator() != null) {
            return extractFromDirect(ctx.directDeclarator());
        }
        return null;
    }
}
