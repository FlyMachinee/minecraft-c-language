package net.flymachine.minecraftclanguage.content.logic.compiler.frontend.c99.helper.initHelper;

import com.mojang.datafixers.util.Either;
import net.flymachine.minecraftclanguage.content.logic.compiler.common.constant.*;
import net.flymachine.minecraftclanguage.content.logic.compiler.common.staticInit.StaticInit;
import net.flymachine.minecraftclanguage.content.logic.compiler.common.staticInit.ZeroInit;
import net.flymachine.minecraftclanguage.content.logic.compiler.common.type.*;
import net.flymachine.minecraftclanguage.content.logic.compiler.frontend.c99.TypeCheckingPass;
import net.flymachine.minecraftclanguage.content.logic.compiler.frontend.c99.ast.node.*;
import net.flymachine.minecraftclanguage.content.logic.compiler.frontend.c99.helper.constexprHelper.ConstantEvaluator;
import net.flymachine.minecraftclanguage.content.logic.errorHandle.DiagnosticReporter;
import net.flymachine.minecraftclanguage.content.logic.errorHandle.SourceLocation;
import org.apache.commons.lang3.mutable.MutableBoolean;

import java.util.ArrayList;
import java.util.List;

public final class InitializerHelper {

    private final DiagnosticReporter reporter;
    private final TypeCheckingPass typeChecker;
    private final ConstantEvaluator constantEvaluator;

    public InitializerHelper(DiagnosticReporter reporter, TypeCheckingPass typeChecker) {
        this.reporter = reporter;
        this.typeChecker = typeChecker;
        this.constantEvaluator = new ConstantEvaluator(typeChecker.getSymbolTable());
    }

    private static SingleInitializerNode makeSingleInit(Constant constant) {
        ConstantNode node = new ConstantNode(null, constant);
        node.expType = constant.getType();
        return new SingleInitializerNode(node);
    }

    private static SingleInitializerNode makeSingleInit(ExpressionNode exp) {
        return new SingleInitializerNode(exp);
    }

    /**
     * 将可能含指代符、且省略元素的初始化器规范化为不含指代符（或为顺序指代符）、且不省略元素的初始化器，并进行可能的类型转换
     *
     * @param typeToInit 要初始化的类型
     * @param init       要规范的初始化器
     * @return 规范后的初始化器
     */
    public InitializerNode normalize(Type typeToInit, InitializerNode init) {
        if (init instanceof CompoundInitializerNode cin) {
            InitializerNode full = zeroInitializer(typeToInit);
            normalizeHelper(typeToInit, full, cin);
            return full;
        }

        SingleInitializerNode sin = (SingleInitializerNode) init;
        if (typeToInit.isAggregate()) {
            // 聚合类型初始化
            String msg = "invalid initializer";
            reporter.error(init.getWholeLocation(), msg);
            return null;
        } else {
            // 标量初始化
            // 标量的初始化式必须是单个表达式，可选地以花括号环绕
            sin.exp = typeChecker.checkExpressionAndDecay(sin.exp);
            if (!sin.exp.expType.isError()) {
                sin.exp.expType = sin.exp.expType.removeConst();

                if (!typeChecker.validConvertAsIfByAssignment(sin.exp, typeToInit)) {
                    String msg = "incompatible types when initializing type '" +
                                 reporter.white(typeToInit.typename()) +
                                 "' using type '" + reporter.white(sin.exp.expType.typename()) + "'";
                    reporter.error(sin.exp.wholeLoc, msg);
                } else {
                    sin.exp = typeChecker.convertTo(sin.exp, typeToInit);
                }
            }
            return sin;
        }
    }

    /**
     * 根据初始化器的内容，确定数组类型的第一维大小
     *
     * @param arrayType 要确定大小的数组类型
     * @param init      初始化器
     */
    public long determineArraySize(ArrayType arrayType, CompoundInitializerNode init) {
        if (arrayType.size().value() > 0) {
            return arrayType.size().value();
        }

        Designation cursor = new Designation(arrayType);
        long size = 0;
        for (DesignationInitializerNode din : init.inits) {
            // 如果有指代符，将其应用
            if (!din.designators.isEmpty()) {
                reporter.suppressDiagnostics();
                Designation newCursor = makeDesignation(arrayType, din.designators);
                reporter.unsuppressDiagnostics();
                if (newCursor != null) {
                    cursor = newCursor;
                }
            }
            size = Math.max(size, ((ArrayDesignator) cursor.getDesignators().get(0)).index() + 1);

            // 处理初始化器
            if (din.initializer instanceof SingleInitializerNode) {
                cursor.expand();
            }
            cursor.next();
        }
        return size;
    }

    private void checkOverwrite(InitializerNode toBeWritten, InitializerNode newInit, MutableBoolean warned) {
        if (toBeWritten instanceof SingleInitializerNode sin) {
            if (sin.exp.wholeLoc != null) {
                // 覆盖了先前的初始化结果
                if (!warned.booleanValue()) {
                    reporter.warning(newInit.getWholeLocation(), "initializer overwrites previous one");
                    warned.setValue(true);
                }
                reporter.note(sin.exp.wholeLoc, "previous initializer, may not be evaluated for side-effects");
            }
        } else if (toBeWritten instanceof CompoundInitializerNode cin) {
            for (DesignationInitializerNode child : cin.inits) {
                checkOverwrite(child.initializer, newInit, warned);
            }
        }
    }

    private void normalizeHelper(Type typeToInit, InitializerNode full, CompoundInitializerNode init) {
        if (full instanceof SingleInitializerNode sin) {
            normalizeHelper(typeToInit, sin, init);
        } else if (full instanceof CompoundInitializerNode cin) {
            normalizeHelper(typeToInit, cin, init);
        } else {
            throw new IllegalStateException("unexpected initializer type: " + full.getClass().getName());
        }
    }

    private void normalizeHelper(Type typeToInit, SingleInitializerNode full, CompoundInitializerNode init) {
        // 标量初始化
        // 标量的初始化式必须是单个表达式，可选地以花括号环绕
        DesignationInitializerNode first = init.inits.get(0);
        // 不允许有指代符
        makeDesignation(typeToInit, first.designators);

        InitializerNode firstInit = first.initializer;
        if (firstInit instanceof CompoundInitializerNode cin) {
            String msg = "braces around scalar initializer";
            reporter.warning(cin.getWholeLocation(), msg);
            normalizeHelper(typeToInit, full, cin);
        } else {
            SingleInitializerNode firstSingle = (SingleInitializerNode) firstInit;
            // 覆盖检查
            checkOverwrite(full, firstSingle, new MutableBoolean(false));
            // 类型转换
            firstSingle.exp = typeChecker.checkExpressionAndDecay(firstSingle.exp);
            if (!firstSingle.exp.expType.isError()) {
                firstSingle.exp.expType = firstSingle.exp.expType.removeConst();
                if (!typeChecker.validConvertAsIfByAssignment(firstSingle.exp, typeToInit)) {
                    String msg = "incompatible types when initializing type '" +
                                 reporter.white(typeToInit.typename()) +
                                 "' using type '" + reporter.white(firstSingle.exp.expType.typename()) + "'";
                    reporter.error(init.wholeLoc, msg);
                } else {
                    firstSingle.exp = typeChecker.convertTo(firstSingle.exp, typeToInit);
                    full.exp = firstSingle.exp;
                }
            }
        }
        for (int i = 1; i < init.inits.size(); ++i) {
            DesignationInitializerNode extra = init.inits.get(i);
            String msg = "excess elements in scalar initializer";
            reporter.error(extra.initializer.getWholeLocation(), msg);
        }

    }


    private void normalizeHelper(Type typeToInit, CompoundInitializerNode full, CompoundInitializerNode init) {
        // 聚合类型初始化
        Designation cursor = new Designation(typeToInit);
        for (DesignationInitializerNode din : init.inits) {
            // 如果有指代符，将其应用
            if (!din.designators.isEmpty()) {
                Designation newCursor = makeDesignation(typeToInit, din.designators);
                if (newCursor != null) {
                    cursor = newCursor;
                }
            }

            // 溢出检查
            if (cursor.isEmpty()) {
                String msg = "excess elements in array initializer";
                reporter.error(din.initializer.getWholeLocation(), msg);
                continue;
            }

            // 处理初始化器
            InitializerNode initializer = din.initializer;
            if (initializer instanceof SingleInitializerNode sin) {
                // 普通初始化器
                cursor.expand();
                // 覆盖检查
                checkOverwrite(cursor.fetch(full), sin, new MutableBoolean(false));
                // 类型检查
                Type subtype = cursor.subtype();
                sin.exp = typeChecker.checkExpressionAndDecay(sin.exp);
                if (!(sin.exp.expType instanceof ErrorType)) {
                    sin.exp.expType = sin.exp.expType.removeConst();
                    if (!typeChecker.validConvertAsIfByAssignment(sin.exp, subtype)) {
                        String msg = "incompatible types when initializing type '" +
                                     reporter.white(subtype.typename()) +
                                     "' using type '" + reporter.white(sin.exp.expType.typename()) + "'";
                        reporter.error(sin.getWholeLocation(), msg);
                    } else {
                        sin.exp = typeChecker.convertTo(sin.exp, subtype);
                    }
                }
                cursor.store(full, sin);
                cursor.next();
            } else {
                // 复合初始化器
                CompoundInitializerNode cin = (CompoundInitializerNode) initializer;
                // 覆盖检查
                checkOverwrite(cursor.fetch(full), cin, new MutableBoolean(false));
                // 递归处理
                Type subtype = cursor.subtype();
                if (!subtype.isAggregate()) {
                    String msg = "braces around scalar initializer";
                    reporter.warning(cin.getWholeLocation(), msg);
                }
                InitializerNode newFull = zeroInitializer(subtype);
                normalizeHelper(subtype, newFull, cin);
                cursor.store(full, newFull);
                cursor.next();
            }
        }
    }

    /**
     * 从指代符列表构造指代符序列
     *
     * @param typeToInit  指代符序列所管理的类型
     * @param designators 指代符列表
     * @return 构造得到的指代符序列，或 {@code null} 如果无法构造
     */
    private Designation makeDesignation(Type typeToInit, List<DesignatorNode> designators) {
        Type currentType = typeToInit;
        List<Designator> currentDesignators = new ArrayList<>();

        // 遍历列表
        for (DesignatorNode designator : designators) {
            if (currentType instanceof ArrayType at) {
                // 数组中只允许出现数组指代符
                if (designator instanceof ArrayDesignatorNode adn) {
                    typeChecker.checkExpression(adn.index);

                    // 要求为整数常量
                    Either<Constant, SourceLocation> evalResult = constantEvaluator.tryEvalIntegerConstant(adn.index);
                    long indexValue;
                    if (evalResult.right().isPresent()) {
                        String msg = "array index in initializer must be a constant integer expression";
                        reporter.error(evalResult.right().get(), msg);
                        indexValue = 0;
                    } else {
                        indexValue = evalResult.orThrow().toLong().value();
                    }
                    adn.index = new ConstantNode(adn.index.wholeLoc, new ConstantLong(indexValue));

                    // 检查是否越界
                    long size = at.size().value();
                    if (indexValue < 0 || (size > 0 && indexValue >= size)) {
                        String msg = "array index in initializer exceeds array bounds";
                        reporter.error(adn.index.wholeLoc, msg);
                        indexValue = 0;
                    }

                    currentDesignators.add(new ArrayDesignator(indexValue, at));
                    currentType = at.elementType();
                    continue;
                }
            }

            // 错误处理
            String msg;
            if (designator instanceof ArrayDesignatorNode) {
                msg = "array index in non-array initializer";
            } else {
                throw new IllegalStateException("unexpected designator type: " + designator.getClass().getName());
            }
            reporter.error(designator.getWholeLocation(), msg);
            return null;
        }
        return new Designation(currentDesignators);
    }

    /**
     * 构造空初始化对应类型时，需要的完整初始化器
     *
     * @param t 需要空初始化的类型
     * @return 构造得到的初始化器
     */
    public static InitializerNode zeroInitializer(Type t) {
        if (t instanceof BasicType bt) {
            // 整数类型对象被初始化成无符号的零
            // 浮点类型对象被初始化成正零
            return makeSingleInit(switch (bt.primitive()) {
                case INT -> ConstantInt.ZERO;
                case LONG -> ConstantLong.ZERO;
                case UNSIGNED_INT -> ConstantUnsignedInt.ZERO;
                case UNSIGNED_LONG -> ConstantUnsignedLong.ZERO;
                case DOUBLE -> ConstantDouble.ZERO;
            });
        }
        if (t instanceof PointerType pt) {
            // 指针被初始化成其类型的空指针值
            return makeSingleInit(new ConstantPointer(0, pt.referencedType()));
        }
        // 数组的所有元素、结构体的所有成员及联合体的首个成员递归地空初始化
        if (t instanceof ArrayType at) {
            List<DesignationInitializerNode> list = new ArrayList<>();
            long size = at.size().value();
            for (long i = 0; i < size; ++i) {
                list.add(new DesignationInitializerNode(null, List.of(), zeroInitializer(at.elementType())));
            }
            return new CompoundInitializerNode(null, list);
        }
        throw new UnsupportedOperationException("type '" + t.toString() + "' cannot be initialized");
    }

    /**
     * 构造空初始化对应类型的静态对象时，需要静态初始化器
     *
     * @param t 需要空初始化的类型
     * @return 构造得到的静态初始化器
     */
    public static List<StaticInit> zeroStaticInit(Type t) {
        return List.of(new ZeroInit(t.sizeof()));
    }

    /**
     * 将规范化后的初始化器转换为静态初始化器
     *
     * @param type       要初始化的类型
     * @param normalized 规范化后的初始化器
     * @return 转换得到的静态初始化器
     */
    public List<StaticInit> toStaticInit(Type type, InitializerNode normalized) {
        List<StaticInit> result = new ArrayList<>();
        toStaticInitHelper(type, normalized, result);
        return result;
    }

    private void toStaticInitHelper(Type type, InitializerNode normalized, List<StaticInit> result) {
        if (normalized instanceof SingleInitializerNode sin) {
            ExpressionNode init = sin.exp;
            StaticInit toAppend;

            // 拥有静态存储期的对象的初始化式中使用的表达式，必须是下列表达式之一
            // 算术常量表达式
            // 空指针常量
            // 地址常量表达式
            // 某完整对象类型的地址常量表达式加或减一个整数常量表达式

            // 在类型检查时，已经将初始化表达式转换至对应的类型
            if (type.isArithmetic()) {
                Either<Constant, SourceLocation> evalResult = constantEvaluator.tryEvalArithmeticConstant(init);
                if (evalResult.right().isPresent()) {
                    String msg = "initializer element is not constant";
                    reporter.error(evalResult.right().get(), msg);
                    toAppend = new ZeroInit(type.sizeof());
                } else {
                    Constant constant = evalResult.orThrow();
                    toAppend = constant.toStaticInitOrZero();
                }
            } else {
                if (!(init instanceof ConstantNode constInit)) {
                    String msg = "initializer element is not constant";
                    reporter.error(init.wholeLoc, msg);
                    toAppend = new ZeroInit(type.sizeof());
                } else {
                    toAppend = constInit.value.toStaticInitOrZero();
                }
            }

            if (result.isEmpty()) {
                result.add(toAppend);
            } else {
                StaticInit last = result.get(result.size() - 1);
                if (last instanceof ZeroInit zi && toAppend instanceof ZeroInit zi2) {
                    // 合并连续的零初始化
                    zi.expand(zi2.bytes());
                } else {
                    result.add(toAppend);
                }
            }
            return;
        }
        if (normalized instanceof CompoundInitializerNode cin) {
            if (type instanceof ArrayType at) {
                for (DesignationInitializerNode din : cin.inits) {
                    toStaticInitHelper(at.elementType(), din.initializer, result);
                }
                return;
            }
        }
        throw new UnsupportedOperationException("type '" + type.toString() + "' cannot be converted to static init");
    }

}