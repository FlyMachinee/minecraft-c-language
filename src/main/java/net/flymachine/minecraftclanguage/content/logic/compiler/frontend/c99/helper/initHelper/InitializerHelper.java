package net.flymachine.minecraftclanguage.content.logic.compiler.frontend.c99.helper.initHelper;

import com.mojang.datafixers.util.Either;
import net.flymachine.minecraftclanguage.content.logic.compiler.common.constant.*;
import net.flymachine.minecraftclanguage.content.logic.compiler.common.staticInit.StaticInit;
import net.flymachine.minecraftclanguage.content.logic.compiler.common.staticInit.StringInit;
import net.flymachine.minecraftclanguage.content.logic.compiler.common.staticInit.ZeroInit;
import net.flymachine.minecraftclanguage.content.logic.compiler.common.type.*;
import net.flymachine.minecraftclanguage.content.logic.compiler.frontend.c99.TypeCheckingPass;
import net.flymachine.minecraftclanguage.content.logic.compiler.frontend.c99.ast.node.*;
import net.flymachine.minecraftclanguage.content.logic.compiler.frontend.c99.helper.constexprHelper.ConstantEvaluator;
import net.flymachine.minecraftclanguage.content.logic.errorHandle.DiagnosticReporter;
import net.flymachine.minecraftclanguage.content.logic.errorHandle.SourceLocation;
import org.apache.commons.lang3.mutable.MutableBoolean;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public final class InitializerHelper {

    private final DiagnosticReporter reporter;
    private final TypeCheckingPass typeChecker;
    private final ConstantEvaluator constantEvaluator;

    public InitializerHelper(DiagnosticReporter reporter, TypeCheckingPass typeChecker) {
        this.reporter = reporter;
        this.typeChecker = typeChecker;
        this.constantEvaluator = new ConstantEvaluator(typeChecker.getSymbolTable(), reporter);
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
    public @NotNull InitializerNode normalize(Type typeToInit, InitializerNode init) {
        // 字符串字面量初始化特例
        if (typeToInit instanceof ArrayType at) {
            StringLiteralNode str = extractStringLiteral(init);
            if (str != null) {
                return normalizeStringInit(at, str, init);
            }
        }

        if (init instanceof CompoundInitializerNode cin) {
            InitializerNode full = zeroInitializer(typeToInit);
            normalizeHelper(typeToInit, full, cin);
            return full;
        }

        SingleInitializerNode sin = (SingleInitializerNode) init;
        sin.exp = typeChecker.checkExpressionAndDecay(sin.exp);
        if (typeToInit.isAggregate()) {
            // 聚合类型初始化
            // 对于结构体，可使用对应类型的结构体来初始化
            if (!typeToInit.isStruct()) {
                if (!sin.exp.expType.isError()) { // 避免级联报错
                    String msg = "invalid initializer";
                    reporter.error(init.getWholeLocation(), msg);
                }
                return zeroInitializer(typeToInit);
            }
        }
        // 标量初始化
        if (!sin.exp.expType.isError()) {
            sin.exp.expType = sin.exp.expType.removeConst();

            if (!typeChecker.validConvertAsIfByAssignment(sin.exp, typeToInit)) {
                String msg = "incompatible types when initializing type '" +
                             reporter.white(typeToInit.typename()) +
                             "' using type '" + reporter.white(sin.exp.expType.typename()) + "'";
                reporter.error(sin.exp.wholeLoc, msg);
            } else {
                sin.exp = typeChecker.convertTo(sin.exp, typeToInit);
                return sin;
            }
        }
        return zeroInitializer(typeToInit);
    }

    private @NotNull InitializerNode normalizeStringInit(
        ArrayType at, StringLiteralNode str, InitializerNode original) {
        if (!at.elementType().isCharacter()) {
            typeChecker.checkExpression(str);
            String msg = "cannot initialize array of '" +
                         reporter.white(at.elementType().removeQualifiers().typename()) +
                         "' from a string literal with type array of '" +
                         reporter.white("char") + "'";
            reporter.error(str.getWholeLocation(), msg);
            return zeroInitializer(at);
        }

        long size = at.size().value();
        if (str.literal.length > size) {
            reporter.error(original.getWholeLocation(),
                           "initializer-string for array of '" +
                           reporter.white(at.elementType().removeQualifiers().typename()) +
                           "' is too long");
            return zeroInitializer(at);
        }
        SingleInitializerNode sin = new SingleInitializerNode(str);
        sin.exp.expType = at;
        return sin;
    }

    /**
     * 根据初始化器的内容，确定数组类型的第一维大小
     *
     * @param at   要确定大小的数组类型
     * @param init 初始化器
     * @return 数组长度；如果无法推导，返回 0
     */
    public long determineArraySize(ArrayType at, InitializerNode init) {
        // 字符串字面量初始化 char 数组
        if (at.elementType().isCharacter()) {
            StringLiteralNode str = extractStringLiteral(init);
            if (str != null) {
                return str.literal.length + 1;
            }
        }
        // 复合初始化器
        if (init instanceof CompoundInitializerNode cin) {
            return determineArraySize(at, cin);
        }
        return 0;
    }

    /**
     * 根据初始化器的内容，确定数组类型的第一维大小
     *
     * @param arrayType 要确定大小的数组类型
     * @param init      初始化器
     */
    private long determineArraySize(ArrayType arrayType, CompoundInitializerNode init) {
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
                reporter.clearSuppressDiagnostics();
                if (newCursor != null) {
                    cursor = newCursor;
                }
            }
            size = Math.max(size, ((ArrayDesignator) cursor.getDesignators().get(0)).index() + 1);

            // 字符串字面量
            if (din.initializer instanceof SingleInitializerNode sin && sin.exp instanceof StringLiteralNode) {
                cursor.next();
                continue;
            }

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
                    reporter.error(firstSingle.exp.wholeLoc, msg);
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
                String type = typeToInit.isArray() ? "array" : "struct";
                String msg = "excess elements in " + type + " initializer";
                reporter.error(din.initializer.getWholeLocation(), msg);
                continue;
            }

            InitializerNode initializer = din.initializer;

            // 字符串字面量初始化内层 char[]
            if (initializer instanceof SingleInitializerNode sin && sin.exp instanceof StringLiteralNode str) {
                Type subtype = cursor.subtype();
                if (subtype instanceof ArrayType innerArray) {
                    if (innerArray.elementType().isCharacter()) {
                        long innerSize = innerArray.size().value();
                        if (str.literal.length > innerSize) {
                            String msg = "initializer-string for array of '" +
                                         reporter.white(innerArray.elementType().removeQualifiers().typename()) +
                                         "' is too long";
                            reporter.error(sin.getWholeLocation(), msg);
                        }
                        sin.exp.expType = innerArray;
                        // 覆盖检查
                        checkOverwrite(cursor.fetch(full), sin, new MutableBoolean(false));
                        cursor.store(full, sin);
                    } else {
                        typeChecker.checkExpression(str);
                        String msg = "cannot initialize array of '" +
                                     reporter.white(innerArray.elementType().removeQualifiers().typename()) +
                                     "' from a string literal with type array of '" +
                                     reporter.white("char") + "'";
                        reporter.error(sin.getWholeLocation(), msg);
                    }
                    cursor.next();
                    continue;
                }
            }

            // 处理初始化器
            if (initializer instanceof SingleInitializerNode sin) {
                Type subtype = cursor.subtype();
                sin.exp = typeChecker.checkExpressionAndDecay(sin.exp);

                // 特例检查，若是聚合类型的初始化器，且类型兼容，则直接使用该聚合类型的初始化器来初始化
                if (subtype.isAggregate() && !(sin.exp.expType instanceof ErrorType)) {
                    sin.exp.expType = sin.exp.expType.removeConst();
                    Type exprType = sin.exp.expType;

                    boolean compatible;
                    if (subtype.isStruct()) {
                        // 结构体/联合体：类型必须兼容
                        compatible = subtype.removeQualifiers().isCompatible(exprType.removeQualifiers());
                    } else {
                        compatible = false; // 数组不允许通过赋值来初始化
                    }

                    if (compatible) {
                        checkOverwrite(cursor.fetch(full), sin, new MutableBoolean(false));
                        cursor.store(full, sin);
                        cursor.next();
                        continue;
                    }
                }

                // 普通初始化器
                cursor.expand();
                // 覆盖检查
                checkOverwrite(cursor.fetch(full), sin, new MutableBoolean(false));
                // 类型检查
                if (!(sin.exp.expType instanceof ErrorType)) {
                    sin.exp.expType = sin.exp.expType.removeConst();
                    if (!typeChecker.validConvertAsIfByAssignment(sin.exp, subtype)) {
                        String msg = "incompatible types when initializing type '" +
                                     reporter.white(subtype.typename()) +
                                     "' using type '" + reporter.white(sin.exp.expType.typename()) + "'";
                        reporter.error(sin.getWholeLocation(), msg);
                    } else {
                        sin.exp = typeChecker.convertTo(sin.exp, subtype);
                        cursor.store(full, sin);
                    }
                }
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

    private static StringLiteralNode extractStringLiteral(InitializerNode init) {
        if (init instanceof SingleInitializerNode sin && sin.exp instanceof StringLiteralNode str) {
            return str;
        }
        if (init instanceof CompoundInitializerNode cin && cin.inits.size() == 1) {
            DesignationInitializerNode din = cin.inits.get(0);
            if (din.designators.isEmpty() && din.initializer instanceof SingleInitializerNode sin
                && sin.exp instanceof StringLiteralNode str) {
                return str;
            }
        }
        return null;
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
            if (currentType instanceof ArrayType at && designator instanceof ArrayDesignatorNode adn) {
                // 数组中只允许出现数组指代符
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
            if (currentType instanceof StructType st && designator instanceof MemberDesignatorNode mdn) {
                // 结构体中只允许出现成员指代符
                String memberName = mdn.member.name;
                if (!st.hasField(memberName)) {
                    String msg = "'" + reporter.white(st.typename()) + "' has no member named '"
                                 + reporter.white(memberName) + "'";
                    reporter.error(mdn.member.wholeLoc, msg);
                    memberName = st.info().fields().get(0).name;
                }

                currentDesignators.add(new MemberDesignator(memberName, st));
                currentType = st.info().getField(memberName).orElseThrow().type;
                continue;
            }

            // 错误处理
            String msg;
            if (designator instanceof ArrayDesignatorNode) {
                msg = "array index in non-array initializer";
            } else if (designator instanceof MemberDesignatorNode) {
                msg = "field name not in record or union initializer";
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
                case CHAR, SIGNED_CHAR -> ConstantChar.ZERO;
                case UNSIGNED_CHAR -> ConstantUnsignedChar.ZERO;
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
        if (t instanceof StructType st) {
            List<DesignationInitializerNode> list = new ArrayList<>();
            for (Field field : st.info().fields()) {
                list.add(new DesignationInitializerNode(null, List.of(), zeroInitializer(field.type)));
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

        // 字符串字面量初始化 char[]
        if (type instanceof ArrayType at &&
            normalized instanceof SingleInitializerNode sin && sin.exp instanceof StringLiteralNode str) {

            if (!at.elementType().isCharacter()) {
                result.add(new ZeroInit(type.sizeof()));
                return;
            }

            long arraySize = at.size().value();
            long literalLength = str.literal.length;

            // 大小在先前已经检查过了，此次直接截断
            // 字面量截断至数组大小
            byte[] truncatedLiteral = str.literal;
            if (literalLength > arraySize) {
                truncatedLiteral = new byte[(int) arraySize];
                System.arraycopy(str.literal, 0, truncatedLiteral, 0, (int) arraySize);
            }

            // char[5] <- "hello" = "hello"
            // char[6] <- "hello" = "hello\0"
            // char[8] <- "hello" = "hello\0" + [0]*2

            result.add(new StringInit(truncatedLiteral, literalLength < arraySize));

            long nullBytes = arraySize - literalLength - 1;
            if (nullBytes > 0) {
                result.add(new ZeroInit(nullBytes));
            }
            return;
        }

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
                var evalResult = constantEvaluator.tryEvalArithmeticConstant(init);
                if (evalResult.right().isPresent()) {
                    String msg = "initializer element is not constant";
                    reporter.error(evalResult.right().get(), msg);
                    toAppend = new ZeroInit(type.sizeof());
                } else {
                    toAppend = evalResult.orThrow().toStaticInitOrZero();
                }
            } else if (type.isPointer()) {
                // 省去了空指针常量的检查，因为在类型检查时都被 cast 到了对应的指针类型
                var evalResult = constantEvaluator.tryEvalAddressConstant(init);
                if (evalResult.right().isPresent()) {
                    String msg = "initializer element is not constant";
                    reporter.error(evalResult.right().get(), msg);
                    toAppend = new ZeroInit(8);
                } else {
                    toAppend = evalResult.orThrow().toStaticInitOrZero();
                }
            } else {
                reporter.error(init.getWholeLocation(), "initializer element is not constant");
                toAppend = new ZeroInit(type.sizeof());
            }

            if (toAppend instanceof ZeroInit zi) {
                appendZeroInit(result, zi.bytes());
            } else {
                result.add(toAppend);
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
            if (type instanceof StructType st) {
                long offset = 0;
                List<Field> fields = st.info().fields();
                int index = 0;
                for (DesignationInitializerNode din : cin.inits) {
                    Field f = fields.get(index);
                    // 当前初始化器填充该字段
                    // 先进行 padding
                    long fieldOffset = f.offset;
                    if (fieldOffset > offset) {
                        appendZeroInit(result, fieldOffset - offset);
                        offset = fieldOffset;
                    }
                    // 再进行字段初始化
                    toStaticInitHelper(f.type, din.initializer, result);
                    offset += f.type.sizeof();
                    // 初始化下一个字段
                    index++;
                }
                // 尾部 padding
                long structSize = st.info().sizeof();
                if (structSize > offset) {
                    appendZeroInit(result, structSize - offset);
                }
                return;
            }
        }
        throw new UnsupportedOperationException("type '" + type.toString() + "' cannot be converted to static init");
    }

    private void appendZeroInit(List<StaticInit> result, long bytes) {
        if (bytes <= 0) {
            return;
        }
        if (!result.isEmpty()) {
            StaticInit last = result.get(result.size() - 1);
            if (last instanceof ZeroInit zi) {
                zi.expand(bytes);
                return;
            }
        }
        result.add(new ZeroInit(bytes));
    }

}