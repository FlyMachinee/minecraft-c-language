package net.flymachine.minecraftclanguage.content.logic.compiler.common.constant;

import com.mojang.datafixers.util.Either;
import net.flymachine.minecraftclanguage.content.logic.compiler.backend.la64.highLevel.AsmType;
import net.flymachine.minecraftclanguage.content.logic.compiler.common.BinaryOperator;
import net.flymachine.minecraftclanguage.content.logic.compiler.common.Comparison;
import net.flymachine.minecraftclanguage.content.logic.compiler.common.UnaryOperator;
import net.flymachine.minecraftclanguage.content.logic.compiler.common.staticInit.CharInit;
import net.flymachine.minecraftclanguage.content.logic.compiler.common.staticInit.StaticInit;
import net.flymachine.minecraftclanguage.content.logic.compiler.common.type.BasicType;
import net.flymachine.minecraftclanguage.content.logic.compiler.common.type.Type;
import net.flymachine.minecraftclanguage.content.logic.errorHandle.DiagnosticReporter;
import org.jetbrains.annotations.NotNull;

public record ConstantChar(byte value) implements IntegerConstant {

    public static final ConstantChar ZERO = new ConstantChar((byte) 0);

    @Override
    public @NotNull String toString() {
        return value + "C";
    }

    @Override
    public ConstantInt toInt() {
        return new ConstantInt(value);
    }

    @Override
    public ConstantLong toLong() {
        return new ConstantLong(value);
    }

    @Override
    public ConstantUnsignedInt toUnsignedInt() {
        return new ConstantUnsignedInt(value);
    }

    @Override
    public ConstantUnsignedLong toUnsignedLong() {
        return new ConstantUnsignedLong(value);
    }

    @Override
    public ConstantDouble toDouble() {
        return new ConstantDouble(value);
    }

    @Override
    public ConstantChar toChar() {
        return this;
    }

    @Override
    public ConstantUnsignedChar toUnsignedChar() {
        return new ConstantUnsignedChar(value);
    }

    @Override
    public ConstantPointer toPointer(Type referencedType) {
        return new ConstantPointer(value, referencedType);
    }

    @Override
    public StaticInit toStaticInit() {
        return new CharInit(value);
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
    public BasicType getType() {
        return BasicType.CHAR;
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
        return true;
    }

    @Override
    public boolean isPositive() {
        return value >= 0;
    }
}
