package net.flymachine.minecraftclanguage.content.logic.compiler.frontend.c99;

import net.flymachine.minecraftclanguage.content.logic.compiler.common.StorageClassSpecifier;
import net.flymachine.minecraftclanguage.content.logic.compiler.common.type.EnumInfo;
import net.flymachine.minecraftclanguage.content.logic.compiler.common.type.record.RecordInfo;
import net.flymachine.minecraftclanguage.content.logic.compiler.common.type.record.StructInfo;
import net.flymachine.minecraftclanguage.content.logic.compiler.common.type.record.UnionInfo;
import net.flymachine.minecraftclanguage.content.logic.compiler.frontend.c99.ast.AstVisitor;
import net.flymachine.minecraftclanguage.content.logic.compiler.frontend.c99.ast.node.*;
import net.flymachine.minecraftclanguage.content.logic.compiler.frontend.c99.enumerate.TagKind;
import net.flymachine.minecraftclanguage.content.logic.compiler.frontend.c99.helper.scope.ScopeStack;
import net.flymachine.minecraftclanguage.content.logic.errorHandle.DiagnosticReporter;
import net.flymachine.minecraftclanguage.content.logic.errorHandle.SourceLocation;
import org.jetbrains.annotations.Nullable;

/**
 * 将变量的名字替换为唯一的名字，并检查变量的重复定义和未定义使用
 */
public final class IdentifierResolutionPass implements AstVisitor<Void> {

    private final DiagnosticReporter reporter;

    public IdentifierResolutionPass(DiagnosticReporter reporter) {
        this.reporter = reporter;
    }

    private int renameCounter = 0;

    private String makeUniqueName(String base) {
        return base + ".." + (renameCounter++);
    }

    private final ScopeStack<String, IdentifierEntry> scopeStack = new ScopeStack<>();
    private final ScopeStack<String, TagEntry> tagScopeStack = new ScopeStack<>();


    private void enterScope() {
        scopeStack.enterScope();
        tagScopeStack.enterScope();
    }

    private void exitScope() {
        scopeStack.exitScope();
        tagScopeStack.exitScope();
    }

    private boolean inGlobalScope() {
        return scopeStack.inGlobalScope();
    }

    @Override
    public Void visit(ProgramNode node) {
        for (ExternalDeclarationNode externalDeclaration : node.extDecls) {
            externalDeclaration.accept(this);
        }
        return null;
    }

    private void visitStatementLabel(StatementNode node) {
        for (var caseLabel : node.caseLabels) {
            caseLabel.caseValue.accept(this);
        }
    }

    private void visitTypeNode(TypeNode type) {
        if (type instanceof FunctionTypeNode funcType) {
            visitTypeNode(funcType.retType);
            visitFunctionTypeNode(funcType, false);
        }

        if (type instanceof ArrayTypeNode arrayType) {
            if (arrayType.size != null) {
                arrayType.size.accept(this);
            }
            visitTypeNode(arrayType.elementType);
        }

        if (type instanceof PointerTypeNode pointerType) {
            visitTypeNode(pointerType.referencedType);
        }

        if (type instanceof RecordTypeNode<?> recordType) {
            visitRecordTypeNode(recordType);
        }

        if (type instanceof EnumTypeNode enumType) {
            visitEnumTypeNode(enumType);
        }
    }

    private void visitRecordTypeNode(RecordTypeNode<?> recordType) {
        if (recordType.resolved()) {
            return;
        }
        if (recordType.memberDeclarations != null) {
            // struct/union tag? { ... } ...?
            // 结构体/联合体定义，仅在当前作用域中查找
            if (recordType.tag == null) {
                // 匿名结构体/联合体
                bindDummy(recordType);
            } else {
                checkTagInCurrentScope(recordType);
            }
            for (MemberDeclarationNode memberDecl : recordType.memberDeclarations) {
                visitMemberDeclaration(memberDecl);
            }
        } else {
            // struct/union tag
            // 结构体引用/隐式声明
            assert recordType.tag != null;
            resolveTagReference(recordType);
        }
    }

    private void visitEnumTypeNode(EnumTypeNode enumType) {
        if (enumType.resolved()) {
            return;
        }
        if (enumType.enumerators != null) {
            // enum tag? { ... }
            // 枚举定义
            if (enumType.tag == null) {
                // 匿名枚举
                bindDummy(enumType);
            } else {
                checkTagInCurrentScope(enumType);
            }
            for (EnumeratorNode enumerator : enumType.enumerators) {
                if (enumerator.value != null) {
                    enumerator.value.accept(this);
                }
                declareEnumerator(enumerator, enumType);
            }
        } else {
            // enum tag
            assert enumType.tag != null;
            resolveTagReference(enumType);
        }
    }

    private void declareEnumerator(EnumeratorNode enumerator, EnumTypeNode enumType) {
        String name = enumerator.name.name;
        IdentifierEntry previous = scopeStack.declarationInCurrentScope(name).orElse(null);
        if (previous != null) {
            if (previous.kind == SymbolKind.ENUMERATOR) {
                String msg = "redefinition of enumerator '" + reporter.white(name) + "'";
                panicWithPreviousRef(msg, enumerator.name, previous);
            } else {
                panicKindConflict(enumerator.name, previous);
            }
            return;
        }
        scopeStack.declare(name, IdentifierEntry.ofEnumerator(enumerator.name, enumType));
    }


    private void visitMemberDeclaration(MemberDeclarationNode memberDecl) {
        // 结构体成员声明中，每行都至少声明了一个标识符
        // 故这里我们不处理其基类型
        for (MemberDeclaratorNode memberDeclarator : memberDecl.memberDeclarators) {
            visitTypeNode(memberDeclarator.finalType);
        }
    }

    private void visitFunctionTypeNode(FunctionTypeNode funcType, boolean isDefinition) {
        if (funcType.hasNoParameters()) {
            // 参数列表为单独的 void
            return;
        }

        if (!isDefinition) {
            enterScope(); // 函数原型作用域
        }

        for (int i = 0; i < funcType.params.size(); i++) {
            TypeNode type = funcType.paramTypes.get(i);
            visitTypeNode(type);

            IdentifierNode identifier = funcType.params.get(i);
            if (identifier == null) {
                if (isDefinition) {
                    String msg = "ISO C99 does not support omitting parameter names in function definitions";
                    reporter.error(type.getWholeLocation(), msg);
                }
                // 函数声明中允许参数不具名
                continue;
            }

            // 这里认为参数声明是定义，为 No Linkage
            visitDeclarationLike(identifier, type, null, true);
        }

        if (!isDefinition) {
            exitScope(); // 函数原型作用域
        }
    }

    @Override
    public Void visit(FunctionDefinitionNode node) {
        if (node.funcType instanceof FunctionTypeNode funcType) {
            // 先处理其返回类型
            visitTypeNode(funcType.retType);
        } else {
            // 函数定义不是函数类型，将在类型检查中报错，这里先递归处理其类型
            visitTypeNode(node.funcType);
        }

        // 函数体作用域
        enterScope();
        // 再处理形参列表
        if (node.funcType instanceof FunctionTypeNode funcType) {
            visitFunctionTypeNode(funcType, true);
        }

        // 声明结束，回过头来定义函数名本身
        // 回到全局作用域
        var bodyScope = scopeStack.popScope();
        var tagBodyScope = tagScopeStack.popScope();
        visitDeclarationLike(node.id, node.funcType, node.storageClass, true);

        // 回到函数体作用域
        scopeStack.pushScope(bodyScope);
        tagScopeStack.pushScope(tagBodyScope);

        // 处理函数体
        for (BlockItemNode item : node.body.blockItems) {
            item.accept(this);
        }

        // 退出函数体作用域
        exitScope();
        return null;
    }

    @Override
    public Void visit(ReturnNode node) {
        visitStatementLabel(node);
        if (node.exp != null) {
            node.exp.accept(this);
        }
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
        if (node.initDeclarators.isEmpty()) {
            if (node.baseType instanceof RecordTypeNode<?> recordType) {
                if (recordType.memberDeclarations == null) {
                    // struct/union S;
                    assert recordType.tag != null;
                    checkTagInCurrentScope(recordType);
                    return null;
                } else {
                    // struct/record tag? { ... };
                    if (recordType.tag == null) {
                        // 匿名结构体，且没有相关联的标识符
                        String msg = "unnamed struct/union that defines no instances";
                        reporter.warning(recordType.wholeLoc, msg);
                    }
                    visitTypeNode(recordType);
                }
            }
            if (node.baseType instanceof EnumTypeNode enumType) {
                // enum E; 和 enum E { ... }; 都走 visitTypeNode
                // 引用形式会在 resolveTagReference 里根据是否已定义决定合法或报错
                // 定义形式会在 visitEnumTypeNode 里注册枚举常量
                visitTypeNode(enumType);
                return null;
            }
        }

        for (InitDeclaratorNode initDecl : node.initDeclarators) {
            visitTypeNode(initDecl.finalType);
            visitDeclarationLike(
                initDecl.id, initDecl.finalType, node.storageClass,
                !(initDecl.finalType instanceof FunctionTypeNode) && initDecl.init != null);
            if (initDecl.init != null) {
                initDecl.init.accept(this);
            }
        }
        return null;
    }

    private void visitDeclarationLike(
        IdentifierNode id, TypeNode type, @Nullable StorageClassSpecifierNode storageClass, boolean defined) {

        String name = id.name;
        IdentifierEntry previous = scopeStack.declarationOf(name).orElse(null);

        boolean isFunction = type instanceof FunctionTypeNode;
        SymbolKind currentKind = isFunction ? SymbolKind.FUNCTION : SymbolKind.OBJECT;
        boolean isExtern = storageClass != null
                           && storageClass.storageClass.equals(StorageClassSpecifier.EXTERN);
        boolean currentHasLinkage = isFunction || inGlobalScope() || isExtern;

        // 符号类型检查：同作用域，或双方都有链接（引用同一实体）
        if (previous != null && previous.kind != currentKind) {
            boolean sameScope = scopeStack.declaredInCurrentScope(name);
            boolean bothLinkage = previous.hasLinkage && currentHasLinkage;
            if (sameScope || bothLinkage) {
                panicKindConflict(id, previous);
                return;
            }
        }

        if (isFunction) {
            // 函数声明，始终有链接
            if (!inGlobalScope() && storageClass != null &&
                storageClass.storageClass.equals(StorageClassSpecifier.STATIC)) {
                // 块作用域函数声明为 static 非法
                String msg = "invalid storage class for function '" + reporter.white(name) + "'";
                reporter.error(id.wholeLoc, msg);
            }
            // 函数声明不需要重命名
            if (previous != null && previous.defined && previous.hasLinkage) {
                // 先前有声明、有链接且为定义，引用先前的定义
                assert previous.kind == SymbolKind.FUNCTION;
                scopeStack.declare(name, IdentifierEntry.ofFunction(previous.id, previous.t, true));
            } else {
                // 先前无声明，或先前声明不是定义，或先前声明无链接
                // 引入新符号
                assert !(previous != null && previous.hasLinkage && previous.kind != SymbolKind.FUNCTION);
                scopeStack.declare(name, IdentifierEntry.ofFunction(id, type, defined));
            }
        } else {
            // 变量声明，如果在全局作用域则有链接，否则没有链接
            if (inGlobalScope()) {
                // 有链接
                // 全局变量不需要重命名
                // 这里不考虑可能的类型冲突，随后在类型检查中处理
                if (previous == null || !previous.defined) {
                    // 先前无声明，或先前声明不是定义
                    assert !(previous != null && previous.kind != SymbolKind.OBJECT);
                    scopeStack.declare(name, IdentifierEntry.ofObject(id, type, true, defined));
                } else {
                    // 否则采用先前的定义，目前在全局作用域，那么我们不需要再次定义，使用先前的即可
                    assert previous.kind == SymbolKind.OBJECT;
                }
                return;
            }

            // 块作用域变量
            if (previous != null && scopeStack.declaredInCurrentScope(name) &&
                !(previous.hasLinkage && currentHasLinkage)) {
                // 先前有声明，而且是当前作用域的
                // 若先前声明有链接，并且当前声明是 extern，则允许重定义
                // 除此之外，声明链接冲突
                String msg;
                if (!previous.hasLinkage) {
                    // 先前的声明无链接
                    if (!isExtern) {
                        // 当前的声明无链接
                        msg = (defined ? "redefinition" : "redeclaration") + " of '" + reporter.white(name) +
                              "' with no linkage";
                    } else {
                        // 当前的声明是 extern，有链接
                        msg = defined ?
                            "redefinition of '" + reporter.white(name) + "'" :
                            "extern declaration of '" + reporter.white(name) +
                            "' follows declaration with no linkage";
                    }
                } else {
                    // 先前的声明有链接，则当前声明无链接
                    msg = (defined ? "redefinition" : "redeclaration") + " of '" + reporter.white(name) +
                          "' with no linkage";
                }
                panicWithPreviousRef(msg, id, previous);
                return;
            }
            // 声明不冲突或先前无声明（不考虑类型）
            if (isExtern) {
                // 当前声明是 extern，有链接，不需要重命名
                if (previous != null && previous.defined && previous.hasLinkage) {
                    assert previous.kind == SymbolKind.OBJECT;
                    // 先前有声明、有链接且为定义，引用先前的定义
                    scopeStack.declare(name, IdentifierEntry.ofObject(previous.id, previous.t, true, true));
                } else {
                    assert !(previous != null && previous.hasLinkage && previous.kind != SymbolKind.OBJECT);
                    // 先前无声明，或先前声明不是定义，或先前声明无链接
                    // 引入新符号
                    scopeStack.declare(name, IdentifierEntry.ofObject(id, type, true, defined));
                }
            } else {
                // 无链接，需要重命名
                id.name = makeUniqueName(name);
                scopeStack.declare(name, IdentifierEntry.ofObject(id, type, false, defined));
            }
        }
    }

    @Override
    public Void visit(ExpressionStatementNode node) {
        visitStatementLabel(node);
        node.exp.accept(this);
        return null;
    }

    @Override
    public Void visit(NullStatementNode node) {
        visitStatementLabel(node);
        return null;
    }

    @Override
    public Void visit(VariableNode node) {
        String name = node.id.name;
        IdentifierEntry entry = scopeStack.declarationOf(name).orElse(null);
        if (entry == null) {
            reporter.error(node.wholeLoc, "'" + reporter.white(name) + "' undeclared");
        } else {
            node.id.name = entry.id.name;
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

    private void visitSubstatement(StatementNode node) {
        if (node instanceof CompoundStatementNode || node instanceof IfStatementNode ||
            node instanceof SwitchStatementNode || node instanceof WhileLoopNode || node instanceof ForLoopNode) {
            node.accept(this);
        } else {
            enterScope();
            node.accept(this);
            exitScope();
        }
    }

    @Override
    public Void visit(IfStatementNode node) {
        visitStatementLabel(node);
        enterScope();
        node.cond.accept(this);
        visitSubstatement(node.thenStmt);
        if (node.elseStmt != null) {
            visitSubstatement(node.elseStmt);
        }
        exitScope();
        return null;
    }

    @Override
    public Void visit(ConditionalExpressionNode node) {
        node.cond.accept(this);
        node.thenExp.accept(this);
        node.elseExp.accept(this);
        return null;
    }

    @Override
    public Void visit(GotoNode node) {
        visitStatementLabel(node);
        return null;
    }

    @Override
    public Void visit(CompoundStatementNode node) {
        visitStatementLabel(node);
        enterScope();
        for (BlockItemNode item : node.blockItems) {
            item.accept(this);
        }
        exitScope();
        return null;
    }

    @Override
    public Void visit(BreakNode node) {
        visitStatementLabel(node);
        return null;
    }

    @Override
    public Void visit(ContinueNode node) {
        visitStatementLabel(node);
        return null;
    }

    @Override
    public Void visit(WhileLoopNode node) {
        visitStatementLabel(node);
        enterScope();
        if (node.isDoWhile) {
            visitSubstatement(node.body);
            node.cond.accept(this);
        } else {
            node.cond.accept(this);
            visitSubstatement(node.body);
        }
        exitScope();
        return null;
    }

    @Override
    public Void visit(ForLoopNode node) {
        visitStatementLabel(node);
        enterScope();
        if (node.init != null) {
            node.init.accept(this);
        }
        if (node.cond != null) {
            node.cond.accept(this);
        }
        if (node.step != null) {
            node.step.accept(this);
        }
        visitSubstatement(node.body);
        exitScope();
        return null;
    }

    @Override
    public Void visit(SwitchStatementNode node) {
        visitStatementLabel(node);
        enterScope();
        node.exp.accept(this);
        visitSubstatement(node.body);
        exitScope();
        return null;
    }

    @Override
    public Void visit(FunctionCallNode node) {
        node.func.accept(this);
        for (ExpressionNode arg : node.args) {
            arg.accept(this);
        }
        return null;
    }

    @Override
    public Void visit(ConstantNode node) {
        return null;
    }

    @Override
    public Void visit(CastExpressionNode node) {
        visitTypeNode(node.targetType);
        return node.exp.accept(this);
    }

    @Override
    public Void visit(AddressOfNode node) {
        node.exp.accept(this);
        return null;
    }

    @Override
    public Void visit(DereferenceNode node) {
        node.exp.accept(this);
        return null;
    }

    @Override
    public Void visit(SubscriptNode node) {
        node.lhs.accept(this);
        node.rhs.accept(this);
        return null;
    }

    @Override
    public Void visit(StringLiteralNode node) {
        return null;
    }

    @Override
    public Void visit(SizeOfNode node) {
        node.exp.accept(this);
        return null;
    }

    @Override
    public Void visit(SizeOfTypeNode node) {
        visitTypeNode(node.type);
        return null;
    }

    @Override
    public Void visit(CommaExpressionNode node) {
        node.lhs.accept(this);
        node.rhs.accept(this);
        return null;
    }

    @Override
    public Void visit(MemberAccessNode node) {
        node.base.accept(this);
        return null;
    }

    @Override
    public Void visit(PointerMemberAccessNode node) {
        node.pointer.accept(this);
        return null;
    }

    // =============================================
    // 普通标识符处理逻辑，包括变量、函数、枚举常量的声明和引用
    // =============================================

    private enum SymbolKind {
        OBJECT, FUNCTION, ENUMERATOR
    }

    private static class IdentifierEntry {
        public IdentifierNode id;
        public TypeNode t;
        public boolean hasLinkage;
        public boolean defined; // 仅用于信息打印，不参与逻辑检查
        public SymbolKind kind;

        public IdentifierEntry(IdentifierNode id, TypeNode t, boolean hasLinkage, boolean defined, SymbolKind kind) {
            this.id = id;
            this.t = t;
            this.hasLinkage = hasLinkage;
            this.defined = defined;
            this.kind = kind;
        }

        public static IdentifierEntry ofObject(IdentifierNode id, TypeNode t, boolean hasLinkage, boolean defined) {
            return new IdentifierEntry(id, t, hasLinkage, defined, SymbolKind.OBJECT);
        }

        public static IdentifierEntry ofFunction(IdentifierNode id, TypeNode t, boolean defined) {
            return new IdentifierEntry(id, t, true, defined, SymbolKind.FUNCTION);
        }

        public static IdentifierEntry ofEnumerator(IdentifierNode id, TypeNode t) {
            return new IdentifierEntry(id, t, false, true, SymbolKind.ENUMERATOR);
        }
    }

    private void panicWithPreviousRef(String msg, IdentifierNode id, IdentifierEntry previous) {
        reporter.error(id.wholeLoc, msg);
        msg = "previous " + (previous.defined ? "definition" : "declaration") + " of '" +
              reporter.white(id.name) + "' with type '" +
              reporter.white(previous.t.typename()) + "'";
        reporter.note(previous.id.wholeLoc, msg);
    }

    private void panicKindConflict(IdentifierNode id, IdentifierEntry previous) {
        String msg = "'" + reporter.white(id.name) + "' redeclared as different kind of symbol";
        panicWithPreviousRef(msg, id, previous);
    }

    // =================================================
    // tag 处理逻辑，包括 struct/union/enum 的 tag 声明和引用
    // =================================================

    private abstract static sealed class TagEntry permits RecordTagEntry, EnumTagEntry {
        final String tag;
        final TagKind kind;
        final SourceLocation location;

        TagEntry(String tag, TagKind kind, SourceLocation location) {
            this.tag = tag;
            this.kind = kind;
            this.location = location;
        }

        abstract void bindTo(TaggedTypeNode node);
    }

    private static final class RecordTagEntry extends TagEntry {
        private final RecordInfo info;

        RecordTagEntry(RecordInfo info, TagKind kind, SourceLocation location) {
            super(info.tag(), kind, location);
            if (kind != TagKind.STRUCT && kind != TagKind.UNION) {
                throw new IllegalArgumentException("RecordTagEntry requires STRUCT or UNION");
            }
            this.info = info;
        }

        @Override
        void bindTo(TaggedTypeNode node) {
            if (!(node instanceof RecordTypeNode<?> rt)) {
                throw new IllegalArgumentException("cannot bind RecordTagEntry to " + node.getClass().getSimpleName());
            }
            rt.resolve(info);
        }
    }

    private static final class EnumTagEntry extends TagEntry {
        private final EnumInfo info;

        EnumTagEntry(EnumInfo info, SourceLocation location) {
            super(info.tag(), TagKind.ENUM, location);
            this.info = info;
        }

        @Override
        void bindTo(TaggedTypeNode node) {
            if (!(node instanceof EnumTypeNode et)) {
                throw new IllegalArgumentException("cannot bind EnumTagEntry to " + node.getClass().getSimpleName());
            }
            et.resolve(info);
        }
    }

    private void checkTagInCurrentScope(TaggedTypeNode current) {
        assert current.tag != null;
        String tag = current.tag.name;
        TagEntry previous = tagScopeStack.declarationInCurrentScope(tag).orElse(null);
        checkTagReference(current, previous, true);
    }

    private void resolveTagReference(TaggedTypeNode current) {
        assert current.tag != null;
        String tag = current.tag.name;
        TagEntry previous = tagScopeStack.declarationOf(tag).orElse(null);
        checkTagReference(current, previous, current.tagKind() != TagKind.ENUM);
    }

    private void checkTagReference(TaggedTypeNode current, @Nullable TagEntry previous, boolean createIfMissing) {
        assert current.tag != null;
        if (previous != null) {
            if (previous.kind != current.tagKind()) {
                panicWrongTagKind(current, previous);
                bindDummy(current);
                return;
            }
            previous.bindTo(current);
            return;
        }

        if (createIfMissing) {
            TagEntry entry = createTagEntry(current);
            entry.bindTo(current);
            tagScopeStack.declare(current.tag.name, entry);
        } else {
            reporter.error(current.tag.wholeLoc,
                           "ISO C forbids forward references to '" + reporter.white("enum") + "' types");
            bindDummy(current);
        }
    }

    private void panicWrongTagKind(TaggedTypeNode current, TagEntry previous) {
        assert current.tag != null;
        String tag = current.tag.name;
        String msg = "'" + reporter.white(tag) + "' defined as wrong kind of tag";
        reporter.error(current.tag.wholeLoc, msg);
        reporter.note(previous.location, "previous declaration");
    }

    private void bindDummy(TaggedTypeNode current) {
        createTagEntry(current).bindTo(current);
    }

    private TagEntry createTagEntry(TaggedTypeNode node) {
        String tag = node.tag == null ? null : node.tag.name;
        SourceLocation loc = node.tag == null ? node.wholeLoc : node.tag.wholeLoc;

        if (node instanceof StructTypeNode) {
            return new RecordTagEntry(new StructInfo(tag), TagKind.STRUCT, loc);
        }
        if (node instanceof UnionTypeNode) {
            return new RecordTagEntry(new UnionInfo(tag), TagKind.UNION, loc);
        }
        if (node instanceof EnumTypeNode) {
            return new EnumTagEntry(new EnumInfo(tag), loc);
        }
        throw new IllegalStateException("Unknown TaggedTypeNode: " + node.getClass().getName());
    }
}
