package net.flymachine.minecraftclanguage.content.logic.compiler.common.constant;

import net.flymachine.minecraftclanguage.content.logic.compiler.backend.la64.highLevel.AsmType;
import net.flymachine.minecraftclanguage.content.logic.compiler.common.BinaryOperator;
import net.flymachine.minecraftclanguage.content.logic.compiler.common.Comparison;
import net.flymachine.minecraftclanguage.content.logic.compiler.common.UnaryOperator;
import net.flymachine.minecraftclanguage.content.logic.compiler.common.staticInit.StaticInit;
import net.flymachine.minecraftclanguage.content.logic.compiler.common.type.BasicType;
import net.flymachine.minecraftclanguage.content.logic.compiler.common.type.PointerType;
import net.flymachine.minecraftclanguage.content.logic.compiler.common.type.Type;

public sealed interface Constant
    permits ConstantInt, ConstantLong, ConstantUnsignedInt, ConstantUnsignedLong, ConstantDouble, ConstantPointer {

    ConstantInt toInt();

    ConstantLong toLong();

    ConstantUnsignedInt toUnsignedInt();

    ConstantUnsignedLong toUnsignedLong();

    ConstantDouble toDouble();

    ConstantPointer toPointer(Type referencedType);

    default Constant castTo(Type type) {
        if (type instanceof BasicType bt) {
            return switch (bt.primitive()) {
                case INT -> toInt();
                case LONG -> toLong();
                case UNSIGNED_INT -> toUnsignedInt();
                case UNSIGNED_LONG -> toUnsignedLong();
                case DOUBLE -> toDouble();
            };
        }
        if (type instanceof PointerType pt) {
            return toPointer(pt.referencedType());
        }
        throw new UnsupportedOperationException("Unsupported type for constant cast: " + type);
    }

    StaticInit toStaticInit();

    Constant apply(UnaryOperator op);

    Constant apply(BinaryOperator op, Constant rhs);

    ConstantInt apply(Comparison cmp, Constant rhs);

    boolean isZero();

    Type getType();

    AsmType getAsmType();

    boolean isNullPointer();
}
