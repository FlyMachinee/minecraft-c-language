package net.flymachine.minecraftclanguage.content.logic.compiler.frontend.c99;

import com.mojang.datafixers.util.Either;
import net.flymachine.minecraftclanguage.content.logic.compiler.common.AssignmentOperator;
import net.flymachine.minecraftclanguage.content.logic.compiler.common.BinaryOperator;
import net.flymachine.minecraftclanguage.content.logic.compiler.common.Comparison;
import net.flymachine.minecraftclanguage.content.logic.compiler.common.UnaryOperator;
import net.flymachine.minecraftclanguage.content.logic.compiler.common.constant.*;
import net.flymachine.minecraftclanguage.content.logic.compiler.common.staticInit.StringInit;
import net.flymachine.minecraftclanguage.content.logic.compiler.common.type.ArrayType;
import net.flymachine.minecraftclanguage.content.logic.compiler.common.type.BasicType;
import net.flymachine.minecraftclanguage.content.logic.compiler.common.type.PointerType;
import net.flymachine.minecraftclanguage.content.logic.compiler.common.type.Type;
import net.flymachine.minecraftclanguage.content.logic.compiler.common.util.UndefinedBehaviourUtil;
import net.flymachine.minecraftclanguage.content.logic.compiler.frontend.c99.ast.ExpressionBoolVisitor;
import net.flymachine.minecraftclanguage.content.logic.compiler.frontend.c99.ast.ExpressionVisitor;
import net.flymachine.minecraftclanguage.content.logic.compiler.frontend.c99.ast.StatementVisitor;
import net.flymachine.minecraftclanguage.content.logic.compiler.frontend.c99.ast.node.*;
import net.flymachine.minecraftclanguage.content.logic.compiler.frontend.c99.helper.initHelper.InitializerHelper;
import net.flymachine.minecraftclanguage.content.logic.compiler.ir.*;
import net.flymachine.minecraftclanguage.content.logic.errorHandle.DiagnosticReporter;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class AstToTacLowerer implements
    StatementVisitor, ExpressionVisitor<AstToTacLowerer.ExpEvalResult>, ExpressionBoolVisitor {

    private final SymbolTable symbolTable;
    private final DiagnosticReporter reporter;

    public AstToTacLowerer(SymbolTable symbolTable, DiagnosticReporter reporter) {
        this.symbolTable = symbolTable;
        this.reporter = reporter;
    }

    public TacProgram lower(ProgramNode program) {
        List<TacTopLevel> topLevels = new ArrayList<>();
        for (ExternalDeclarationNode extDecl : program.extDecls) {
            if (extDecl instanceof FunctionDefinitionNode funcDef) {
                topLevels.add(lowerFunc(funcDef));
            }
        }
        // 遍历符号表，将需要本编译单元内初始化的全局变量进行定义
        for (SymbolTable.Entry entry : symbolTable.getEntries()) {
            SymbolTable.Entry.IdentifierAttr attr = entry.attr;
            if (attr instanceof SymbolTable.Entry.StaticAttr staticAttr) {
                SymbolTable.Entry.StaticAttr.DefinitionType defType = staticAttr.defType;
                if (defType instanceof SymbolTable.Entry.StaticAttr.Defined defined) {
                    topLevels.add(new TacStaticVariable(
                        entry.id.name, staticAttr.global, entry.type, defined.init()));
                } else if (defType instanceof SymbolTable.Entry.StaticAttr.Tentative) {
                    topLevels.add(new TacStaticVariable(
                        entry.id.name, staticAttr.global, entry.type, InitializerHelper.zeroStaticInit(entry.type)));
                }
            } else if (attr instanceof SymbolTable.Entry.ConstantAttr constAttr) {
                topLevels.add(new TacStaticConstant(entry.id.name, entry.type, constAttr.init));
            }
        }
        return new TacProgram(topLevels);
    }

    private int tempVarCounter = 0;

    private TacVariable makeTempVar(Type type) {
        String name = "tmp." + (tempVarCounter++);
        TacVariable var = new TacVariable(name);
        symbolTable.put(
            name, new SymbolTable.Entry(
                new IdentifierNode(null, name), null, type,
                SymbolTable.Entry.AutoAttr.INSTANCE));
        return var;
    }

    private int labelCounter = 0;

    private String makeLabel() {
        return "label_" + (labelCounter++);
    }

    private String makeLabel(String prefix) {
        return prefix + "_" + (labelCounter++);
    }

    // 提取变量，避免传参
    private List<TacInstruction> instructions;

    private void emitTac(TacInstruction instruction) {
        instructions.add(instruction);
    }

    private TacFunction lowerFunc(FunctionDefinitionNode funcDef) {
        instructions = new ArrayList<>();
        funcDef.body.accept(this);
        emitTacReturn(new TacConstant(ConstantInt.ZERO));
        boolean global = symbolTable.get(funcDef.id.name).attr.isGlobal();
        FunctionTypeNode functionType = (FunctionTypeNode) funcDef.funcType;
        if (functionType.hasNoParameters()) {
            return new TacFunction(funcDef.id.name, global, List.of(), instructions);
        } else {
            List<String> parameters = functionType.params.stream().map(param -> param.name).toList();
            return new TacFunction(funcDef.id.name, global, parameters, instructions);
        }
    }

    private void lowerBlockItem(BlockItemNode blockItem) {
        if (blockItem instanceof StatementBlockItemNode stmt) {
            lowerStatement(stmt.stmt);
        } else if (blockItem instanceof DeclarationBlockItemNode decl) {
            lowerDecl(decl.decl);
        } else {
            throw new RuntimeException("Unknown instruction: " + blockItem.toString());
        }
    }

    private void lowerDecl(DeclarationNode decl) {
        // 一定为块作用域
        // 无存储类且有初始化时，生成初始化三地址码
        if (decl.storageClass == null) {
            for (InitDeclaratorNode initDecl : decl.initDeclarators) {
                if (initDecl.init != null) {
                    Type t = initDecl.finalType.getType();
                    String name = initDecl.id.name;
                    if (t.isAggregate()) {
                        initializeBlockScopeObject(name, t, initDecl.init, 0);
                    } else {
                        emitTacCopy(
                            evalAndLvalueConvert(((SingleInitializerNode) initDecl.init).exp), new TacVariable(name));
                    }
                }
            }
        }
    }

    private void initializeBlockScopeObject(String target, Type t, InitializerNode init, long offset) {
        // 初始化块作用域变量
        if (!t.isAggregate()) {
            SingleInitializerNode singleInit = (SingleInitializerNode) init;
            TacValue val = evalAndLvalueConvert(singleInit.exp);
            emitTac(new TacCopyToOffset(val, target, offset));
            return;
        }

        // 初始化聚合类型
        // 特殊情况检查，字符串字面量初始化数组
        // 由先前的类型检查保证
        if (init instanceof SingleInitializerNode sin) {
            ArrayType at = (ArrayType) t;
            StringLiteralNode str = (StringLiteralNode) sin.exp;
            byte[] data = new byte[(int) at.size().value()];
            System.arraycopy(str.literal, 0, data, 0, Math.min(data.length, str.literal.length));
            emitTac(new TacCopyByteArrayToOffset(data, target, offset));
            return;
        }

        CompoundInitializerNode compoundInit = (CompoundInitializerNode) init;
        if (t instanceof ArrayType at) {
            long size = at.elementType().sizeof();
            for (DesignationInitializerNode designatedInit : compoundInit.inits) {
                initializeBlockScopeObject(target, at.elementType(), designatedInit.initializer, offset);
                offset += size;
            }
            return;
        }

        throw new IllegalStateException("Control should never reach here");
    }

    private void visitBefore(StatementNode stmt) {
        // 为活跃的 goto 标签生成标签
        for (int i = stmt.gotoLabels.size() - 1; i >= 0; i--) {
            StatementNode.GotoLabelInfo info = stmt.gotoLabels.get(i);
            if (info.active) {
                emitTacLabel(info.label.name);
            }
        }

        // case 和 default
        for (int i = stmt.caseLabels.size() - 1; i >= 0; i--) {
            StatementNode.CaseLabelInfo info = stmt.caseLabels.get(i);
            emitTacLabel("case_" + info.caseValue + "_" + info.switchLabel);
        }
        if (!stmt.defaultLabels.isEmpty()) {
            StatementNode.DefaultLabelInfo info = stmt.defaultLabels.get(0);
            emitTacLabel("default_" + info.switchLabel);
        }
    }

    private void lowerStatement(StatementNode stmt) {
        visitBefore(stmt);
        stmt.accept(this);
    }

    private BoolGenResult lowerBoolean(ExpressionNode exp, String jumpTarget, boolean inverse) {
        return exp.accept(this, jumpTarget, inverse);
    }

    @Override
    public void visit(ReturnNode ret) {
        TacValue returnValue = evalAndLvalueConvert(ret.exp);
        emitTacReturn(returnValue);
    }

    @Override
    public void visit(ExpressionStatementNode expStmt) {
        eval(expStmt.exp);
    }

    @Override
    public void visit(IfStatementNode ifStmt) {
        if (ifStmt.elseStmt != null) {
            // if (cond) thenStmt else elseStmt
            // =>
            // if (!cond) goto else
            // thenStmt
            // goto end
            // else:
            // elseStmt
            // end:
            String labelElse = makeLabel("else");
            switch (lowerBoolean(ifStmt.cond, labelElse, true)) {
                case ALWAYS_JUMP -> {
                    if (!ifStmt.thenStmt.containsActiveLabel()) {
                        // 当 then 子语句没有活跃的 goto 标签，将其优化
                        emitTacLabel(labelElse);
                        lowerStatement(ifStmt.elseStmt);
                        return;
                    }
                    // 否则生成无条件跳转
                    emitTacJump(labelElse);
                }
                case NEVER_JUMP -> {
                    if (!ifStmt.elseStmt.containsActiveLabel()) {
                        // 当 else 子语句没有活跃的 goto 标签，将其优化
                        lowerStatement(ifStmt.thenStmt);
                        emitTacLabel(labelElse);
                        return;
                    }
                }
            }
            String labelEndIf = makeLabel("endif");
            lowerStatement(ifStmt.thenStmt);
            emitTacJump(labelEndIf);
            emitTacLabel(labelElse);
            lowerStatement(ifStmt.elseStmt);
            emitTacLabel(labelEndIf);
        } else {
            // if (cond) thenStmt
            // =>
            // if (!cond) goto end
            // thenStmt
            // end:
            String labelEndIf = makeLabel("endif");
            if (lowerBoolean(ifStmt.cond, labelEndIf, true) == BoolGenResult.ALWAYS_JUMP) {
                if (!ifStmt.thenStmt.containsActiveLabel()) {
                    // 当 then 子语句没有活跃的 goto 标签，将其优化
                    emitTacLabel(labelEndIf);
                    return;
                }
                // 否则生成无条件跳转
                emitTacJump(labelEndIf);
            }
            lowerStatement(ifStmt.thenStmt);
            emitTacLabel(labelEndIf);
        }
    }

    @Override
    public void visit(GotoNode gotoStmt) {
        // 为 goto 语句生成无条件跳转
        emitTacJump(gotoStmt.target.name);
    }

    @Override
    public void visit(CompoundStatementNode compoundStmt) {
        for (BlockItemNode item : compoundStmt.blockItems) {
            lowerBlockItem(item);
        }
    }

    @Override
    public void visit(BreakNode breakStmt) {
        emitTacJump("break_" + breakStmt.loopOrSwitchLabel);
    }

    @Override
    public void visit(ContinueNode continueStmt) {
        emitTacJump("continue_" + continueStmt.loopLabel);
    }

    @Override
    public void visit(WhileLoopNode whileLoop) {
        // while (cond) body
        // =>
        //   if (!cond) goto break_label <=====
        // begin:
        //   body
        // continue_label:
        //   if (cond) goto begin:
        // break_label:

        // do body while (cond);
        // =>
        // begin:
        //   body
        // continue_label:
        //   if (cond) goto begin:
        // break_label:

        String labelBegin = makeLabel(whileLoop.isDoWhile ? "do_while_begin" : "while_begin");
        String labelContinue = "continue_" + whileLoop.loopLabel;
        String labelBreak = "break_" + whileLoop.loopLabel;

        if (!whileLoop.isDoWhile) {
            // while 循环需要在循环前生成跳转到结尾处的条件测试指令
            if (lowerBoolean(whileLoop.cond, labelBreak, true) == BoolGenResult.ALWAYS_JUMP) {
                // 始终跳转，则条件始终为 0
                if (whileLoop.body.containsActiveLabel()) {
                    // 若含有活跃标签，则循环体不能优化，只能生成无条件跳转
                    emitTacJump(labelBreak);
                    // 继续处理循环体
                } else {
                    // 直接优化掉循环体，返回即可
                    return;
                }
            }
        }
        emitTacLabel(labelBegin);
        lowerStatement(whileLoop.body);
        emitTacLabel(labelContinue);
        if (lowerBoolean(whileLoop.cond, labelBegin, false) == BoolGenResult.ALWAYS_JUMP) {
            // 始终跳转，生成无条件跳转
            emitTacJump(labelBegin);
        }
        emitTacLabel(labelBreak);
    }

    @Override
    public void visit(ForLoopNode forLoop) {
        // for (init; cond; step) body
        // =>
        //   init
        //   if (!cond) goto break_label
        // begin:
        //   body
        // continue_label:
        //   step
        //   if (cond) goto begin
        // break_label:
        String labelBegin = makeLabel("for_begin");
        String labelContinue = "continue_" + forLoop.loopLabel;
        String labelBreak = "break_" + forLoop.loopLabel;

        if (forLoop.init != null) {
            if (forLoop.init instanceof ForInitDeclarationNode decl) {
                lowerDecl(decl.decl);
            } else if (forLoop.init instanceof ForInitExpressionNode expr) {
                eval(expr.exp);
            } else {
                throw new RuntimeException("unexpected init node in for loop: " + forLoop.init.getClass());
            }
        }
        if (forLoop.cond != null) {
            if (lowerBoolean(forLoop.cond, labelBreak, true) == BoolGenResult.ALWAYS_JUMP) {
                // 始终跳转，则条件始终为 0
                if (forLoop.body.containsActiveLabel()) {
                    // 若含有活跃标签，则循环体不能优化，只能生成无条件跳转
                    emitTacJump(labelBreak);
                    // 继续处理循环体
                } else {
                    // 直接优化掉循环体，返回即可
                    return;
                }
            }
        } // 否则条件缺省，始终视为真，则不跳转
        emitTacLabel(labelBegin);
        lowerStatement(forLoop.body);
        emitTacLabel(labelContinue);
        if (forLoop.step != null) {
            eval(forLoop.step);
        }
        if (forLoop.cond != null) {
            if (lowerBoolean(forLoop.cond, labelBegin, false) == BoolGenResult.ALWAYS_JUMP) {
                // 始终跳转，生成无条件跳转
                emitTacJump(labelBegin);
            }
        } else {
            // 条件缺省，视为始终为真
            emitTacJump(labelBegin);
        }
        emitTacLabel(labelBreak);
    }

    @Override
    public void visit(SwitchStatementNode switchStmt) {
        // switch (exp) body   {case: [0, 1, 2, ...], default=yes/no}
        // =>
        //   tmp = exp
        //   if (tmp == 0) goto case0
        //   if (tmp == 1) goto case1
        //   goto default (if default=yes)
        //   goto break (if default=no)
        //   body
        // break:
        String labelBreak = "break_" + switchStmt.switchLabel;
        String defaultLabel = "default_" + switchStmt.switchLabel;

        TacValue res = evalAndLvalueConvert(switchStmt.exp);
        if (res instanceof TacConstant constant) {
            // 常量，进行优化
            long value = constant.value.toLong().value();
            if (switchStmt.caseValues.containsKey(value)) {
                // 匹配到 case 标签
                emitTacJump("case_" + value + "_" + switchStmt.switchLabel);
            } else if (switchStmt.defaultLabel != null) {
                // 没有匹配到 case 标签但有 default 标签
                emitTacJump(defaultLabel);
            } else {
                // 没有匹配到 case 标签且没有 default 标签，直接跳转到 break

                // 如果此时 body 没有可能跳入的 goto 标签，则可以优化掉 body 和 break 标签
                if (!switchStmt.body.containsActiveLabel()) {
                    return;
                }
                emitTacJump(labelBreak);
            }
        } else {
            // 非常量，生成比较指令
            for (SwitchStatementNode.CaseLabelInfo caseInfo : switchStmt.caseValues.values()) {
                String caseLabel = "case_" + caseInfo.caseValue + "_" + switchStmt.switchLabel;
                TacValue caseValue = new TacConstant(((ConstantNode) caseInfo.caseValue).value);
                emitTacJumpIfComparison(Comparison.EQUAL, res, caseValue, caseLabel, false);
            }
            if (switchStmt.defaultLabel != null) {
                emitTacJump(defaultLabel);
            } else {
                emitTacJump(labelBreak);
            }
        }
        lowerStatement(switchStmt.body);
        emitTacLabel(labelBreak);

    }

    @Override
    public void visit(NullStatementNode nullStmt) {
    }

    public sealed interface ExpEvalResult { }

    record PlainOperand(TacValue object) implements ExpEvalResult {
        public PlainOperand(Constant constant) {
            this(new TacConstant(constant));
        }
    }

    record DereferencedPointer(TacAddressDescriptor addr) implements ExpEvalResult { }

    // 不可能是左值
    record PointerValue(TacAddressDescriptor addr) implements ExpEvalResult { }

    // 不可能是左值
    record PlainFunctionPointer(String name) implements ExpEvalResult { }

    private ExpEvalResult eval(ExpressionNode exp) {
        return exp.accept(this);
    }

    private TacValue evalAndLvalueConvert(ExpressionNode exp) {
        return toPlainValue(eval(exp), exp.expType);
    }

    private TacAddressDescriptor evalAsAddressDescriptor(ExpressionNode exp) {
        return toAddressDescriptor(eval(exp), exp.expType);
    }

    private TacValue toPlainValue(ExpEvalResult res, Type expType) {
        if (res instanceof PlainOperand plainRes) {
            return plainRes.object();
        }
        if (res instanceof DereferencedPointer derefPtr) {
            TacVariable obj = makeTempVar(expType);
            emitTacLoad(derefPtr.addr(), obj);
            return obj;
        }
        if (res instanceof PointerValue ptrVal) {
            return evalAddressDescriptor(ptrVal.addr(), expType);
        }
        if (res instanceof PlainFunctionPointer funcPtr) {
            TacVariable funcPtrVal = makeTempVar(expType);
            emitTacGetAddress(new TacVariable(funcPtr.name), funcPtrVal);
            return funcPtrVal;
        }
        throw new IllegalStateException("control should never reach here");
    }

    private TacAddressDescriptor toAddressDescriptor(ExpEvalResult res, Type expType) {
        if (res instanceof PlainOperand plainRes) {
            return new TacAddressDescriptor(plainRes.object());
        }
        if (res instanceof DereferencedPointer derefPtr) {
            TacVariable obj = makeTempVar(expType);
            emitTacLoad(derefPtr.addr(), obj);
            return new TacAddressDescriptor(obj);
        }
        if (res instanceof PointerValue ptrVal) {
            return ptrVal.addr();
        }
        throw new IllegalStateException("control should never reach here");
    }

    private TacValue evalAddressDescriptor(TacAddressDescriptor addr, Type pointerType) {
        addr = addr.fold();

        // 计算地址描述符的值
        if (addr.hasIndex()) {
            // val = base + index * scale + offset
            TacVariable tmp = makeTempVar(pointerType);
            emitTacAddPtr(addr.base(), addr.index(), addr.scale(), tmp);
            if (addr.offset() != 0) {
                TacVariable finalResult = makeTempVar(pointerType);
                emitTacAdd(tmp, new TacConstant(new ConstantLong(addr.offset())), finalResult);
                return finalResult;
            } else {
                return tmp;
            }
        } else {
            // val = base + offset
            if (addr.offset() != 0) {
                TacVariable result = makeTempVar(pointerType);
                emitTacAdd(addr.base(), new TacConstant(new ConstantLong(addr.offset())), result);
                return result;
            } else {
                return addr.base();
            }
        }
    }

    private TacAddressDescriptor pointerAdd(TacAddressDescriptor addr, Type srcPointerType, TacValue index,
        long scale) {
        addr = addr.fold();
        if (addr.hasIndex()) {
            TacValue ptr = evalAddressDescriptor(addr, srcPointerType);
            addr = new TacAddressDescriptor(ptr);
        }
        return (new TacAddressDescriptor(addr.base(), index, scale, addr.offset())).fold();
    }

    @Override
    public ExpEvalResult visit(ConstantNode constant) {
        return new PlainOperand(constant.value);
    }

    @Override
    public ExpEvalResult visit(UnaryExpressionNode unaryExp) {
        TacValue src = evalAndLvalueConvert(unaryExp.exp);
        if (src instanceof TacConstant constant) {
            Either<Constant, String> reduced = constant.value.tryApply(unaryExp.op.op, reporter);
            if (reduced.right().isPresent()) {
                reporter.warning(unaryExp.op.wholeLoc, reduced.right().get());
            } else {
                return new PlainOperand(reduced.orThrow());
            }
        }
        if (unaryExp.op.op == UnaryOperator.POSITIVE) {
            // +a => a
            // 一元加和一元减都首先在其操作数上应用整数提升，然后一元加返回提升后的值
            // 整数提升在类型检查阶段已经完成
            return new PlainOperand(src);
        }
        TacVariable dst = makeTempVar(unaryExp.expType);
        emitTac(new TacUnaryOperation(unaryExp.op.op, src, dst));
        return new PlainOperand(dst);
    }

    /**
     * 短路逻辑运算的求值
     * <p>
     * && 和 || 的结构完全一致，只是：
     * <ul>
     *   <li>&&: 遇到 0 短路，短路结果 0，默认结果 1</li>
     *   <li>||: 遇到 1 短路，短路结果 1，默认结果 0</li>
     * </ul>
     */
    private ExpEvalResult evalLogicalShortCircuit(BinaryExpressionNode exp) {
        boolean isAnd = exp.op.op == BinaryOperator.LOGICAL_AND;
        Constant shortResult = isAnd ? ConstantInt.ZERO : ConstantInt.ONE;
        Constant defaultResult = isAnd ? ConstantInt.ONE : ConstantInt.ZERO;
        boolean inverse = isAnd;
        String labelShort = makeLabel(isAnd ? "and_false" : "or_true");

        BoolGenResult lhsResult = lowerBoolean(exp.lhs, labelShort, inverse);
        switch (lhsResult) {
            case ALWAYS_JUMP -> {
                // lhs 短路
                emitTacLabel(labelShort);
                return new PlainOperand(shortResult);
            }
            case NEVER_JUMP -> {
                // lhs 不短路，检查 rhs
                BoolGenResult rhsResult = lowerBoolean(exp.rhs, labelShort, inverse);
                switch (rhsResult) {
                    case ALWAYS_JUMP -> {
                        emitTacLabel(labelShort);
                        return new PlainOperand(shortResult);
                    }
                    case NEVER_JUMP -> {
                        emitTacLabel(labelShort);
                        return new PlainOperand(defaultResult);
                    }
                    case VARIOUS -> { /* 落入通用路径 */ }
                }
            }
            case VARIOUS -> {
                // lhs 未知，检查 rhs
                if (lowerBoolean(exp.rhs, labelShort, inverse) == BoolGenResult.ALWAYS_JUMP) {
                    emitTacLabel(labelShort);
                    return new PlainOperand(shortResult);
                }
                // 否则落入通用路径
            }
        }

        // 通用路径：生成分支赋值
        String labelEnd = makeLabel("eval_end");
        TacVariable dst = makeTempVar(exp.expType);
        emitTacCopy(new TacConstant(defaultResult), dst);
        emitTacJump(labelEnd);
        emitTacLabel(labelShort);
        emitTacCopy(new TacConstant(shortResult), dst);
        emitTacLabel(labelEnd);
        return new PlainOperand(dst);
    }

    @Override
    public ExpEvalResult visit(BinaryExpressionNode binaryExp) {
        BinaryOperator op = binaryExp.op.op;

        // 短路求值
        if (op == BinaryOperator.LOGICAL_AND || op == BinaryOperator.LOGICAL_OR) {
            return evalLogicalShortCircuit(binaryExp);
        }

        // 指针运算
        Type lhsType = binaryExp.lhs.expType;
        Type rhsType = binaryExp.rhs.expType;
        // 指针 + 整数 或 整数 + 指针
        if (op == BinaryOperator.ADD && (lhsType.isPointer() || rhsType.isPointer())) {
            PointerType pointerType;
            ExpressionNode ptr;
            ExpressionNode index;
            if (lhsType instanceof PointerType pt) {
                pointerType = pt;
                ptr = binaryExp.lhs;
                index = binaryExp.rhs;
            } else {
                pointerType = (PointerType) rhsType;
                ptr = binaryExp.rhs;
                index = binaryExp.lhs;
            }
            // ptr + index
            TacAddressDescriptor addr = evalAsAddressDescriptor(ptr);
            TacValue indexVal = evalAndLvalueConvert(index);
            long scale = pointerType.referencedType().sizeof();
            return new PointerValue(pointerAdd(addr, pointerType, indexVal, scale));
        }
        // 指针 - 整数
        if (op == BinaryOperator.SUBTRACT && lhsType instanceof PointerType pointerType && rhsType.isInteger()) {
            // ptr - index
            TacAddressDescriptor addr = evalAsAddressDescriptor(binaryExp.lhs);
            UnaryExpressionNode tmp = new UnaryExpressionNode(
                new UnaryOperatorNode(null, UnaryOperator.NEGATE), binaryExp.rhs);
            tmp.expType = binaryExp.rhs.expType;
            TacValue negIndexVal = evalAndLvalueConvert(tmp);
            long scale = pointerType.referencedType().sizeof();
            return new PointerValue(pointerAdd(addr, pointerType, negIndexVal, scale));
        }
        // 指针 - 指针
        if (op == BinaryOperator.SUBTRACT && lhsType instanceof PointerType lhsPt && rhsType.isPointer()) {
            TacValue lhsVal = evalAndLvalueConvert(binaryExp.lhs);
            TacValue rhsVal = evalAndLvalueConvert(binaryExp.rhs);

            if (lhsVal instanceof TacConstant lhsConstPtr && rhsVal instanceof TacConstant rhsConstPtr &&
                lhsConstPtr.value instanceof ConstantPointer lhsConst &&
                rhsConstPtr.value instanceof ConstantPointer rhsConst) {
                // 常量指针，直接计算差值
                return new PlainOperand(lhsConst.apply(op, rhsConst));
            }

            // 计算指针之间的差值
            TacVariable diff = makeTempVar(binaryExp.expType);
            emitTacSub(lhsVal, rhsVal, diff);
            long scale = lhsPt.referencedType().sizeof();
            if (scale != 1) {
                // 除以大小
                TacVariable result = makeTempVar(binaryExp.expType);
                emitTacDiv(diff, new TacConstant(new ConstantLong(scale)), result);
                return new PlainOperand(result);
            } else {
                return new PlainOperand(diff);
            }
        }

        // 普通求值
        TacValue lhs = evalAndLvalueConvert(binaryExp.lhs);
        TacValue rhs = evalAndLvalueConvert(binaryExp.rhs);

        Optional<String> checkRes = switch (op) {
            case DIVIDE, MODULO -> UndefinedBehaviourUtil.checkDivision(binaryExp.rhs);
            case LEFT_SHIFT -> UndefinedBehaviourUtil.checkBitwiseShift(true, binaryExp.lhs, binaryExp.rhs);
            case RIGHT_SHIFT -> UndefinedBehaviourUtil.checkBitwiseShift(false, binaryExp.lhs, binaryExp.rhs);
            default -> Optional.empty();
        };

        if (checkRes.isPresent()) {
            reporter.warning(binaryExp.op.wholeLoc, checkRes.get());
        } else if (lhs instanceof TacConstant lhsConst && rhs instanceof TacConstant rhsConst) {
            Either<Constant, String> reduced = lhsConst.value.tryApply(op, rhsConst.value, reporter);
            if (reduced.right().isPresent()) {
                reporter.warning(binaryExp.op.wholeLoc, reduced.right().get());
            } else {
                return new PlainOperand(reduced.orThrow());
            }
        }
        TacVariable dst = makeTempVar(binaryExp.expType);
        emitTac(new TacBinaryOperation(op, lhs, rhs, dst));
        return new PlainOperand(dst);
    }

    @Override
    public ExpEvalResult visit(AssignmentNode assignment) {
        // 赋值表达式
        TacValue rhs = evalAndLvalueConvert(assignment.rhs);
        ExpEvalResult dst = eval(assignment.lhs);
        AssignmentOperator op = assignment.op.op;
        TacValue res;

        if (op == AssignmentOperator.ASSIGN) {
            // 普通赋值
            res = rhs;
        } else if (assignment.lhs.expType instanceof PointerType pointerType) {
            // 复合赋值 指针运算
            if (op == AssignmentOperator.ADD_ASSIGN || op == AssignmentOperator.SUBTRACT_ASSIGN) {
                // ptr += index 或 ptr -= index
                // 地址描述符不可能为左值，无需考虑
                TacValue ptr = toPlainValue(dst, assignment.lhs.expType);
                long scale = pointerType.referencedType().sizeof();

                // 进行可能的 cast
                rhs = cast(rhs, BasicType.LONG, assignment.rhs.expType);

                if (op == AssignmentOperator.SUBTRACT_ASSIGN) {
                    // ptr -= index => ptr += -index
                    if (rhs instanceof TacConstant rhsConst) {
                        // 常量，直接取负
                        rhs = new TacConstant(rhsConst.value.apply(UnaryOperator.NEGATE));
                    } else {
                        // 非常量，生成取负指令
                        TacVariable negIndex = makeTempVar(BasicType.LONG);
                        emitTacNeg(rhs, negIndex);
                        rhs = negIndex;
                    }
                }

                TacVariable tmp = makeTempVar(assignment.lhs.expType);
                emitTacAddPtr(ptr, rhs, scale, tmp);
                res = tmp;
            } else {
                throw new IllegalStateException("Control should never reach here");
            }
        } else {
            // 复合赋值 正常运算
            TypeCheckingPass.TypeCheckCompoundAssignmentResult checkRes =
                TypeCheckingPass.typeCheckCompoundAssignment(assignment);
            // 表达式 lhs @= rhs 与 lhs = lhs @ (rhs) 完全相同，但只求值一次 lhs
            // 加载左操作数
            TacValue lhsValue = toPlainValue(dst, assignment.lhs.expType);
            // 进行可能的 cast
            TacValue lhsCastRes = cast(lhsValue, checkRes.lhsTargetType(), assignment.lhs.expType);
            TacValue rhsCastRes = cast(rhs, checkRes.rhsTargetType(), assignment.rhs.expType);
            // 发生计算
            TacVariable tmp = makeTempVar(checkRes.tmpType());
            emitTac(new TacBinaryOperation(assignment.op.op.toBinaryOperator(), lhsCastRes, rhsCastRes, tmp));
            // 结果进行 cast
            res = cast(tmp, assignment.expType, checkRes.tmpType());
        }

        // 写回
        if (dst instanceof PlainOperand objDst) {
            emitTacCopy(res, objDst.object());
            return new PlainOperand(res);
        }
        if (dst instanceof DereferencedPointer derefPtr) {
            emitTacStore(res, derefPtr.addr());
            return new PlainOperand(res);
        }
        throw new IllegalStateException("Control should never reach here");
    }

    @Override
    public ExpEvalResult visit(VariableNode variable) {
        return new PlainOperand(new TacVariable(variable));
    }


    @Override
    public ExpEvalResult visit(IncrementDecrementNode incrementDecrement) {
        // 自增自减表达式
        BinaryOperator op = incrementDecrement.isIncrement ? BinaryOperator.ADD : BinaryOperator.SUBTRACT;
        ExpEvalResult dst = eval(incrementDecrement.operand);
        Type operandType = incrementDecrement.operand.expType;
        TacConstant step;
        if (operandType instanceof BasicType bt) {
            step = new TacConstant(switch (bt.primitive()) {
                case INT, CHAR, SIGNED_CHAR, UNSIGNED_CHAR -> ConstantInt.ONE;
                case LONG -> ConstantLong.ONE;
                case UNSIGNED_INT -> ConstantUnsignedInt.ONE;
                case UNSIGNED_LONG -> ConstantUnsignedLong.ONE;
                case DOUBLE -> ConstantDouble.ONE;
            });
        } else {
            PointerType pt = (PointerType) operandType;
            long scale = pt.referencedType().sizeof();
            step = new TacConstant(new ConstantLong(scale));
        }

        TacVariable storage;
        TacAddressDescriptor derefAddr = null;

        // 加载操作数
        if (dst instanceof PlainOperand objDst) {
            storage = (TacVariable) objDst.object(); // 直接获取
        } else if (dst instanceof DereferencedPointer derefPtr) {
            storage = makeTempVar(operandType);
            emitTacLoad(derefPtr.addr(), storage); // 进行 Load
            derefAddr = derefPtr.addr();
        } else {
            throw new IllegalStateException("Control should never reach here");
        }

        // 后缀保留旧值
        TacVariable oldValue = null;
        if (!incrementDecrement.isPrefix) {
            oldValue = makeTempVar(operandType);
            emitTacCopy(storage, oldValue);
        }

        // 进行自增自减操作
        if (operandType.isCharacter()) {
            // 整数提升
            // tmp = (int) storage; tmp = tmp +/- 1; storage = (char) tmp
            TacValue promoted = cast(storage, BasicType.INT, operandType);
            emitTac(new TacBinaryOperation(op, promoted, step, promoted));
            emitTac(new TacTruncate(promoted, storage));
        } else {
            // 直接操作
            emitTac(new TacBinaryOperation(op, storage, step, storage));
        }

        // 如果是内存位置，写回
        if (derefAddr != null) {
            emitTacStore(storage, derefAddr);
        }

        // 前缀返回新值，后缀返回旧值
        return new PlainOperand(incrementDecrement.isPrefix ? storage : oldValue);
    }

    @Override
    public ExpEvalResult visit(ConditionalExpressionNode condExp) {
        // 条件表达式
        // cond ? a : b
        // =>
        // if (!cond) goto false
        // tmp = a
        // goto end
        // false:
        // tmp = b
        // end:
        // yield tmp
        String labelCondFalse = makeLabel("cond_false");

        // 条件表达式也有求值顺序要求，先求条件值
        switch (lowerBoolean(condExp.cond, labelCondFalse, true)) {
            case ALWAYS_JUMP -> {
                // cond=0，只需求假分支即可
                emitTacLabel(labelCondFalse);
                return eval(condExp.elseExp);
            }
            case NEVER_JUMP -> {
                // cond=1，只需求真分支即可
                ExpEvalResult ret = eval(condExp.thenExp);
                emitTacLabel(labelCondFalse);
                return ret;
            }
        }
        String labelCondEnd = makeLabel("cond_end");
        TacValue thenValue = evalAndLvalueConvert(condExp.thenExp);
        TacVariable dst = makeTempVar(condExp.expType);
        emitTacCopy(thenValue, dst);
        emitTacJump(labelCondEnd);
        emitTacLabel(labelCondFalse);
        TacValue elseValue = evalAndLvalueConvert(condExp.elseExp);
        emitTacCopy(elseValue, dst);
        emitTacLabel(labelCondEnd);
        return new PlainOperand(dst);
    }

    @Override
    public ExpEvalResult visit(FunctionCallNode funcCall) {
        // 函数调用
        // func(arg0, arg1, ...)
        // =>
        // res0 = <eval arg0>
        // res1 = <eval arg1>
        // ...
        // dst = invoke(func, [res0, res1,...])
        // yield dst

        List<TacValue> args = new ArrayList<>();
        for (ExpressionNode arg : funcCall.args) {
            args.add(evalAndLvalueConvert(arg));
        }
        TacVariable dst = makeTempVar(funcCall.expType);

        ExpEvalResult func = eval(funcCall.func);
        if (func instanceof PlainFunctionPointer funcPtr) {
            emitTac(new TacDirectCall(funcPtr.name, args, dst));
        } else {
            TacValue funcVal = toPlainValue(func, funcCall.func.expType);
            emitTac(new TacIndirectCall(funcVal, args, dst));
        }
        return new PlainOperand(dst);
    }

    private TacValue cast(TacValue toCast, Type targetType, Type originType) {
        if (targetType.isCompatible(originType)) {
            return toCast;
        }
        if (toCast instanceof TacConstant constant) {
            return new TacConstant(constant.value.castTo(targetType));
        }
        TacVariable dst = makeTempVar(targetType);

        // 指针视为 unsigned long
        BasicType targetBasic = targetType.isBasic() ? (BasicType) targetType : BasicType.UNSIGNED_LONG;
        BasicType originBasic = originType.isBasic() ? (BasicType) originType : BasicType.UNSIGNED_LONG;

        if (targetBasic.isDouble()) {
            switch (originBasic.primitive()) {
                case SIGNED_CHAR, CHAR, INT, LONG -> emitTac(new TacIntToDouble(toCast, dst));
                case UNSIGNED_CHAR, UNSIGNED_INT, UNSIGNED_LONG -> emitTac(new TacUnsignedIntToDouble(toCast, dst));
            }
            return dst;
        }
        if (originBasic.isDouble()) {
            switch (targetBasic.primitive()) {
                case SIGNED_CHAR, CHAR, INT, LONG -> emitTac(new TacDoubleToInt(toCast, dst));
                case UNSIGNED_CHAR, UNSIGNED_INT, UNSIGNED_LONG -> emitTac(new TacDoubleToUnsignedInt(toCast, dst));
            }
            return dst;
        }

        if (targetBasic.sizeof() == originBasic.sizeof()) {
            emitTacCopy(toCast, dst);
        } else if (targetBasic.sizeof() < originBasic.sizeof()) {
            emitTac(new TacTruncate(toCast, dst));
        } else if (originBasic.isSigned()) {
            emitTac(new TacSignExtend(toCast, dst));
        } else {
            emitTac(new TacZeroExtend(toCast, dst));
        }
        return dst;
    }

    @Override
    public ExpEvalResult visit(CastExpressionNode castExp) {
        TacValue toCast = evalAndLvalueConvert(castExp.exp);
        Type targetType = castExp.targetType.getType();
        Type originType = castExp.exp.expType;

        return new PlainOperand(cast(toCast, targetType, originType));
    }

    @Override
    public ExpEvalResult visit(AddressOfNode addrOf) {
        ExpEvalResult exp = eval(addrOf.exp);
        if (exp instanceof PlainOperand expObj) {
            if (addrOf.exp.expType.isFunction()) {
                // &func
                TacVariable funcId = (TacVariable) expObj.object();
                return new PlainFunctionPointer(funcId.name);
            }
            TacVariable ptr = makeTempVar(addrOf.expType);
            emitTacGetAddress(expObj.object(), ptr);
            return new PlainOperand(ptr);
        }
        if (exp instanceof DereferencedPointer expPtr) {
            // &*ptr => ptr
            return new PointerValue(expPtr.addr());
        }
        throw new IllegalStateException("Control should never reach here");
    }

    @Override
    public ExpEvalResult visit(DereferenceNode deref) {
        ExpEvalResult ptr = eval(deref.exp);
        if (ptr instanceof PlainFunctionPointer funcPtr) {
            return new PlainOperand(new TacVariable(funcPtr.name));
        }
        return new DereferencedPointer(toAddressDescriptor(ptr, deref.exp.expType));
    }

    @Override
    public ExpEvalResult visit(SubscriptNode subscript) {
        // a[i] => *(a + i)
        PointerType pointerType;
        ExpressionNode ptr;
        ExpressionNode index;
        if (subscript.lhs.expType instanceof PointerType pt) {
            pointerType = pt;
            ptr = subscript.lhs;
            index = subscript.rhs;
        } else if (subscript.rhs.expType instanceof PointerType pt) {
            pointerType = pt;
            ptr = subscript.rhs;
            index = subscript.lhs;
        } else {
            throw new IllegalStateException("Control should never reach here");
        }
        TacAddressDescriptor addr = evalAsAddressDescriptor(ptr);
        TacValue indexVal = evalAndLvalueConvert(index);
        long scale = pointerType.referencedType().sizeof();
        return new DereferencedPointer(pointerAdd(addr, pointerType, indexVal, scale));
    }

    private int stringLiteralCounter = 0;

    @Override
    public ExpEvalResult visit(StringLiteralNode string) {
        // 字符串字面量作为表达式，生成一个静态对象
        String name = "string." + stringLiteralCounter++;
        Type t = string.expType;
        symbolTable.put(name,
                        new SymbolTable.Entry(
                            new IdentifierNode(null, name), TypeNode.fromType(t), t,
                            new SymbolTable.Entry.ConstantAttr(new StringInit(string.literal, true))));
        return new PlainOperand(new TacVariable(name));
    }

    @Override
    public BoolGenResult visit(ConstantNode constant, String jumpTarget, boolean inverse) {
        // 常量，生成无条件跳转
        if ((!constant.value.isZero()) ^ inverse) {
            return BoolGenResult.ALWAYS_JUMP;
        } else {
            return BoolGenResult.NEVER_JUMP;
        }
    }

    /**
     * 短路逻辑运算 lowerBoolean 的统一实现
     */
    private BoolGenResult lowerLogicalBoolean(BinaryExpressionNode exp, String jumpTarget, boolean inverse) {
        boolean isAnd = exp.op.op == BinaryOperator.LOGICAL_AND;

        // && 求真 或 || 求假时，短路时不跳转，需要跳转至末尾生成的标签
        // if (a && b)  goto target => if (!a) goto and_false ; if (b)  goto target ; and_false:
        // if !(a || b) goto target => if (a)  goto or_true   ; if (!b) goto target ; or_true:
        // 其余两种情况，短路时跳转，不需要显式生成标签
        // if !(a && b) goto target => if (!a) goto target ; if (!b) goto target
        // if (a || b)  goto target => if (a)  goto target ; if (b)  goto target
        boolean needsLabel = (isAnd != inverse);

        // 第一个短路分支的跳转目标，根据上述分析
        // 需要生成标签时用新标签，否则直接跳到 jumpTarget。
        String shortTarget = needsLabel ? makeLabel(isAnd ? "and_false" : "or_false") : jumpTarget;

        BoolGenResult lhsResult = lowerBoolean(exp.lhs, shortTarget, isAnd);
        BoolGenResult result = switch (lhsResult) {
            case ALWAYS_JUMP ->
                // needsLabel：短路跳到末尾标签，从而不跳到 target，为 NEVER_JUMP
                // !needsLabel：短路直接跳到 target，等价于整体 ALWAYS_JUMP
                needsLabel ? BoolGenResult.NEVER_JUMP : BoolGenResult.ALWAYS_JUMP;
            case NEVER_JUMP ->
                // 左侧恒不短路，整个表达式的结果完全由右侧决定
                lowerBoolean(exp.rhs, jumpTarget, inverse);
            case VARIOUS -> {
                // 左侧未知，继续求右侧，根据右侧的结果进行后续
                BoolGenResult rhsResult = lowerBoolean(exp.rhs, jumpTarget, inverse);
                if (needsLabel) {
                    if (rhsResult == BoolGenResult.ALWAYS_JUMP) {
                        // rhs 恒跳转，补一条显式的跳转
                        // 整体仍有两路径，返回 VARIOUS
                        emitTacJump(jumpTarget);
                        yield BoolGenResult.VARIOUS;
                    } else {
                        // rhs 从不跳转，从而整体永不跳转，返回 NEVER_JUMP
                        // rhs 未知，从而整体未知，返回 VARIOUS
                        yield rhsResult;
                    }
                } else {
                    // rhs 恒跳转，整体 ALWAYS_JUMP
                    // 其他情况，整体未知，返回 VARIOUS
                    yield rhsResult == BoolGenResult.ALWAYS_JUMP ? BoolGenResult.ALWAYS_JUMP : BoolGenResult.VARIOUS;
                }
            }
        };

        if (needsLabel) {
            // 末尾标签生成
            emitTacLabel(shortTarget);
        }
        return result;
    }

    @Override
    public BoolGenResult visit(BinaryExpressionNode binaryExp, String jumpTarget, boolean inverse) {
        switch (binaryExp.op.op) {
            case LOGICAL_AND, LOGICAL_OR -> { return lowerLogicalBoolean(binaryExp, jumpTarget, inverse); }
            case EQUAL, NOT_EQUAL, LESS_THAN, LESS_OR_EQUAL, GREATER_THAN, GREATER_OR_EQUAL -> {
                // 直接生成比较跳转指令，而不是比较置位指令
                Comparison cond = binaryExp.op.op.toComparison();

                TacValue lhs = evalAndLvalueConvert(binaryExp.lhs);
                TacValue rhs = evalAndLvalueConvert(binaryExp.rhs);
                if (lhs instanceof TacConstant lhsConst && rhs instanceof TacConstant rhsConst) {
                    if (lhsConst.value.apply(cond, rhsConst.value).isZero() == inverse) {
                        return BoolGenResult.ALWAYS_JUMP;
                    } else {
                        return BoolGenResult.NEVER_JUMP;
                    }
                }
                emitTacJumpIfComparison(cond, lhs, rhs, jumpTarget, inverse);
                return BoolGenResult.VARIOUS;
            }
        }
        return visitFallback(binaryExp, jumpTarget, inverse);
    }


    @Override
    public BoolGenResult visit(UnaryExpressionNode unaryExp, String jumpTarget, boolean inverse) {
        if (unaryExp.op.op == UnaryOperator.NOT) {
            return unaryExp.exp.accept(this, jumpTarget, !inverse);
        }
        return visitFallback(unaryExp, jumpTarget, inverse);
    }

    private BoolGenResult visitFallback(ExpressionNode exp, String jumpTarget, boolean inverse) {
        // 其他表达式，先求值再与 0 比较跳转
        TacValue value = evalAndLvalueConvert(exp);
        if (value instanceof TacConstant constant) {
            if (constant.value.isZero() == inverse) {
                return BoolGenResult.ALWAYS_JUMP;
            } else {
                return BoolGenResult.NEVER_JUMP;
            }
        }
        if (inverse) {
            emitTacJumpIfZero(value, jumpTarget);
        } else {
            emitTacJumpIfNotZero(value, jumpTarget);
        }
        return BoolGenResult.VARIOUS;
    }

    @Override
    public BoolGenResult visit(VariableNode variable, String jumpTarget, boolean inverse) {
        return visitFallback(variable, jumpTarget, inverse);
    }

    @Override
    public BoolGenResult visit(AssignmentNode assignment, String jumpTarget, boolean inverse) {
        return visitFallback(assignment, jumpTarget, inverse);
    }

    @Override
    public BoolGenResult visit(IncrementDecrementNode incrementDecrement, String jumpTarget, boolean inverse) {
        return visitFallback(incrementDecrement, jumpTarget, inverse);
    }

    @Override
    public BoolGenResult visit(ConditionalExpressionNode condExp, String jumpTarget, boolean inverse) {
        return visitFallback(condExp, jumpTarget, inverse);
    }

    @Override
    public BoolGenResult visit(FunctionCallNode funcCall, String jumpTarget, boolean inverse) {
        return visitFallback(funcCall, jumpTarget, inverse);
    }

    @Override
    public BoolGenResult visit(CastExpressionNode castExp, String jumpTarget, boolean inverse) {
        return visitFallback(castExp, jumpTarget, inverse);
    }

    @Override
    public BoolGenResult visit(AddressOfNode addrOf, String jumpTarget, boolean inverse) {
        return visitFallback(addrOf, jumpTarget, inverse);
    }

    @Override
    public BoolGenResult visit(DereferenceNode deref, String jumpTarget, boolean inverse) {
        return visitFallback(deref, jumpTarget, inverse);
    }

    @Override
    public BoolGenResult visit(SubscriptNode subscript, String jumpTarget, boolean inverse) {
        return visitFallback(subscript, jumpTarget, inverse);
    }

    @Override
    public BoolGenResult visit(StringLiteralNode string, String jumpTarget, boolean inverse) {
        return visitFallback(string, jumpTarget, inverse);
    }

    private void emitTacNeg(TacValue src, TacValue dst) {
        emitTac(new TacUnaryOperation(UnaryOperator.NEGATE, src, dst));
    }

    private void emitTacAdd(TacValue lhs, TacValue rhs, TacValue dst) {
        emitTac(new TacBinaryOperation(BinaryOperator.ADD, lhs, rhs, dst));
    }

    private void emitTacSub(TacValue lhs, TacValue rhs, TacValue dst) {
        emitTac(new TacBinaryOperation(BinaryOperator.SUBTRACT, lhs, rhs, dst));
    }

    private void emitTacMul(TacValue lhs, TacValue rhs, TacValue dst) {
        emitTac(new TacBinaryOperation(BinaryOperator.MULTIPLY, lhs, rhs, dst));
    }

    private void emitTacDiv(TacValue lhs, TacValue rhs, TacValue dst) {
        emitTac(new TacBinaryOperation(BinaryOperator.DIVIDE, lhs, rhs, dst));
    }

    private void emitTacCopy(TacValue src, TacValue dst) {
        emitTac(new TacCopy(src, dst));
    }

    private void emitTacLoad(TacAddressDescriptor addr, TacValue dst) {
        emitTac(new TacLoad(addr, dst));
    }

    private void emitTacStore(TacValue src, TacAddressDescriptor addr) {
        emitTac(new TacStore(src, addr));
    }

    private void emitTacLabel(String label) {
        emitTac(new TacLabel(label));
    }

    private void emitTacJump(String label) {
        emitTac(new TacJump(label));
    }

    private void emitTacJumpIfZero(TacValue value, String label) {
        emitTac(new TacJumpIfZero(value, label));
    }

    private void emitTacJumpIfNotZero(TacValue value, String label) {
        emitTac(new TacJumpIfNotZero(value, label));
    }

    private void emitTacJumpIfComparison(Comparison cond, TacValue lhs, TacValue rhs, String label, boolean inverse) {
        emitTac(new TacJumpIfComparison(cond, lhs, rhs, label, inverse));
    }

    private void emitTacReturn(TacValue value) {
        emitTac(new TacReturn(value));
    }

    private void emitTacGetAddress(TacValue obj, TacValue dst) {
        emitTac(new TacGetAddress(obj, dst));
    }

    private void emitTacAddPtr(TacValue base, TacValue index, long scale, TacValue dst) {
        if (index instanceof TacConstant indexConst) {
            long indexValue = indexConst.value.toLong().value();
            long offset = indexValue * scale;
            if (offset != 0) {
                emitTacAdd(base, new TacConstant(new ConstantLong(offset)), dst);
            } else {
                emitTacCopy(base, dst);
            }
            return;
        }

        if (scale == 1) {
            emitTacAdd(base, index, dst);
        } else if (scale == 2 || scale == 4 || scale == 8 || scale == 16) {
            emitTac(new TacAddPointer(base, index, scale, dst));
        } else {
            TacVariable tmp = makeTempVar(BasicType.LONG);
            emitTacMul(index, new TacConstant(new ConstantLong(scale)), tmp);
            emitTacAdd(base, tmp, dst);
        }
    }
}
