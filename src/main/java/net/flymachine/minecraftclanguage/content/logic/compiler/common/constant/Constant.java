package net.flymachine.minecraftclanguage.content.logic.compiler.common.constant;

import com.mojang.datafixers.util.Either;
import net.flymachine.minecraftclanguage.content.logic.compiler.backend.la64.highLevel.AsmType;
import net.flymachine.minecraftclanguage.content.logic.compiler.common.BinaryOperator;
import net.flymachine.minecraftclanguage.content.logic.compiler.common.Comparison;
import net.flymachine.minecraftclanguage.content.logic.compiler.common.UnaryOperator;
import net.flymachine.minecraftclanguage.content.logic.compiler.common.staticInit.StaticInit;
import net.flymachine.minecraftclanguage.content.logic.compiler.common.staticInit.ZeroInit;
import net.flymachine.minecraftclanguage.content.logic.compiler.common.type.BasicType;
import net.flymachine.minecraftclanguage.content.logic.compiler.common.type.PointerType;
import net.flymachine.minecraftclanguage.content.logic.compiler.common.type.Type;
import net.flymachine.minecraftclanguage.content.logic.compiler.frontend.c99.ast.node.ConstantNode;
import net.flymachine.minecraftclanguage.content.logic.errorHandle.DiagnosticReporter;
import net.flymachine.minecraftclanguage.content.logic.errorHandle.SourceLocation;

public sealed interface Constant permits ConstantDouble, IntegerConstant, PointerConstant {

    ConstantInt toInt();

    ConstantLong toLong();

    ConstantUnsignedInt toUnsignedInt();

    ConstantUnsignedLong toUnsignedLong();

    ConstantDouble toDouble();

    ConstantChar toChar();

    ConstantUnsignedChar toUnsignedChar();

    PointerConstant toPointer(Type referencedType);

    default Constant castTo(Type type) {
        if (type instanceof BasicType bt) {
            return switch (bt.primitive()) {
                case INT -> toInt();
                case LONG -> toLong();
                case UNSIGNED_INT -> toUnsignedInt();
                case UNSIGNED_LONG -> toUnsignedLong();
                case DOUBLE -> toDouble();
                case CHAR, SIGNED_CHAR -> toChar();
                case UNSIGNED_CHAR -> toUnsignedChar();
            };
        }
        if (type instanceof PointerType pt) {
            return toPointer(pt.referencedType());
        }
        throw new UnsupportedOperationException("Unsupported type for constant cast: " + type);
    }

    StaticInit toStaticInit();

    default StaticInit toStaticInitOrZero() {
        if (isZero()) {
            return new ZeroInit(getType().sizeof());
        }
        return toStaticInit();
    }

    Constant apply(UnaryOperator op);

    Constant apply(BinaryOperator op, Constant rhs);

    Either<Constant, String> tryApply(UnaryOperator op, DiagnosticReporter reporter);

    Either<Constant, String> tryApply(BinaryOperator op, Constant rhs, DiagnosticReporter reporter);

    ConstantInt apply(Comparison cmp, Constant rhs);

    boolean isZero();

    Type getType();

    AsmType getAsmType();

    boolean isNullPointer();

    default ConstantNode asNode() {
        return new ConstantNode(null, this);
    }

    default ConstantNode asNode(SourceLocation wholeLoc) {
        return new ConstantNode(wholeLoc, this);
    }
}
