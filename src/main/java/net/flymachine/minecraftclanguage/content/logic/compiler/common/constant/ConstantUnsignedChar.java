package net.flymachine.minecraftclanguage.content.logic.compiler.common.constant;

import com.mojang.datafixers.util.Either;
import net.flymachine.minecraftclanguage.content.logic.compiler.backend.la64.highLevel.AsmType;
import net.flymachine.minecraftclanguage.content.logic.compiler.common.BinaryOperator;
import net.flymachine.minecraftclanguage.content.logic.compiler.common.Comparison;
import net.flymachine.minecraftclanguage.content.logic.compiler.common.UnaryOperator;
import net.flymachine.minecraftclanguage.content.logic.compiler.common.staticInit.StaticInit;
import net.flymachine.minecraftclanguage.content.logic.compiler.common.type.Type;
import net.flymachine.minecraftclanguage.content.logic.errorHandle.DiagnosticReporter;
import org.jetbrains.annotations.NotNull;

public record ConstantUnsignedChar(byte value) implements IntegerConstant {

    public static final ConstantUnsignedChar ZERO = new ConstantUnsignedChar((byte) 0);

    @Override
    public @NotNull String toString() {
        return Byte.toUnsignedInt(value) + "UC";
    }

    @Override
    public ConstantInt toInt() {
        return new ConstantInt(Byte.toUnsignedInt(value));
    }

    @Override
    public ConstantLong toLong() {
        return new ConstantLong(Byte.toUnsignedLong(value));
    }

    @Override
    public ConstantUnsignedInt toUnsignedInt() {
        return new ConstantUnsignedInt(Byte.toUnsignedInt(value));
    }

    @Override
    public ConstantUnsignedLong toUnsignedLong() {
        return new ConstantUnsignedLong(Byte.toUnsignedLong(value));
    }

    @Override
    public ConstantDouble toDouble() {
        return new ConstantDouble(Byte.toUnsignedInt(value));
    }

    @Override
    public ConstantPointer toPointer(Type referencedType) {
        return new ConstantPointer(Byte.toUnsignedLong(value), referencedType);
    }

    @Override
    public StaticInit toStaticInit() {
        return null;
    }

    @Override
    public Constant apply(UnaryOperator op) {
        throw new UnsupportedOperationException("should undergo integer promotion first");
    }

    @Override
    public Constant apply(BinaryOperator op, Constant rhs) {
        throw new UnsupportedOperationException("should undergo integer promotion first");
    }

    @Override
    public Either<Constant, String> tryApply(UnaryOperator op, DiagnosticReporter reporter) {
        throw new UnsupportedOperationException("should undergo integer promotion first");
    }

    @Override
    public Either<Constant, String> tryApply(BinaryOperator op, Constant rhs, DiagnosticReporter reporter) {
        throw new UnsupportedOperationException("should undergo integer promotion first");
    }

    @Override
    public ConstantInt apply(Comparison cmp, Constant rhs) {
        throw new UnsupportedOperationException("should undergo integer promotion first");
    }

    @Override
    public boolean isZero() {
        return value == 0;
    }

    @Override
    public Type getType() {
        return null;
    }

    @Override
    public AsmType getAsmType() {
        return null;
    }

    @Override
    public boolean isNullPointer() {
        return value == 0;
    }

    @Override
    public boolean isSigned() {
        return false;
    }

    @Override
    public boolean isPositive() {
        return value != 0;
    }
}
