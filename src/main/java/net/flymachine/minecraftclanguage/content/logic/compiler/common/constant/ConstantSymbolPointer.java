package net.flymachine.minecraftclanguage.content.logic.compiler.common.constant;

import com.mojang.datafixers.util.Either;
import net.flymachine.minecraftclanguage.content.logic.compiler.backend.la64.highLevel.AsmType;
import net.flymachine.minecraftclanguage.content.logic.compiler.common.BinaryOperator;
import net.flymachine.minecraftclanguage.content.logic.compiler.common.Comparison;
import net.flymachine.minecraftclanguage.content.logic.compiler.common.UnaryOperator;
import net.flymachine.minecraftclanguage.content.logic.compiler.common.staticInit.SymbolInit;
import net.flymachine.minecraftclanguage.content.logic.compiler.common.type.PointerType;
import net.flymachine.minecraftclanguage.content.logic.compiler.common.type.Type;
import net.flymachine.minecraftclanguage.content.logic.errorHandle.DiagnosticReporter;

public record ConstantSymbolPointer(String symbol, long offset, Type referencedType) implements PointerConstant {

    public ConstantSymbolPointer(String symbol, Type referencedType) {
        this(symbol, 0, referencedType);
    }

    @Override
    public ConstantInt toInt() {
        throw new UnsupportedOperationException("Cannot determine value in compile time");
    }

    @Override
    public ConstantLong toLong() {
        throw new UnsupportedOperationException("Cannot determine value in compile time");
    }

    @Override
    public ConstantUnsignedInt toUnsignedInt() {
        throw new UnsupportedOperationException("Cannot determine value in compile time");
    }

    @Override
    public ConstantUnsignedLong toUnsignedLong() {
        throw new UnsupportedOperationException("Cannot determine value in compile time");
    }

    @Override
    public ConstantDouble toDouble() {
        throw new UnsupportedOperationException("Cannot determine value in compile time");
    }

    @Override
    public ConstantChar toChar() {
        throw new UnsupportedOperationException("Cannot determine value in compile time");
    }

    @Override
    public ConstantUnsignedChar toUnsignedChar() {
        throw new UnsupportedOperationException("Cannot determine value in compile time");
    }

    @Override
    public long toByteRepresentation() {
        throw new UnsupportedOperationException("Cannot determine value in compile time");
    }

    @Override
    public ConstantSymbolPointer toPointer(Type referencedType) {
        return new ConstantSymbolPointer(symbol, offset, referencedType);
    }

    @Override
    public SymbolInit toStaticInit() {
        return new SymbolInit(symbol, offset);
    }

    @Override
    public Constant apply(UnaryOperator op) {
        if (op == UnaryOperator.NOT) {
            return ConstantInt.ZERO;
        }
        throw new UnsupportedOperationException("not arithmetic type");
    }

    @Override
    public Constant apply(BinaryOperator op, Constant rhs) {
        PointerType lhsType = getType();
        Type rhsType = rhs.getType();

        return switch (op) {
            case ADD -> {
                if (rhsType.isInteger()) {
                    if (!rhsType.isLong()) {
                        throw new UnsupportedOperationException("Cast to long first");
                    }
                    yield new ConstantSymbolPointer(
                        symbol, offset + rhs.toLong().value() * referencedType.sizeof(), referencedType);
                }
                throw new UnsupportedOperationException("Unsupported operation");
            }
            case SUBTRACT -> {
                if (rhsType.isInteger()) {
                    if (!rhsType.isLong()) {
                        throw new UnsupportedOperationException("Cast to long first");
                    }
                    yield new ConstantSymbolPointer(
                        symbol, offset - rhs.toLong().value() * referencedType.sizeof(), referencedType);
                }
                throw new UnsupportedOperationException("Unsupported operation");
            }
            default -> throw new UnsupportedOperationException("Unsupported operation");
        };
    }

    @Override
    public Either<Constant, String> tryApply(UnaryOperator op, DiagnosticReporter reporter) {
        return Either.left(apply(op));
    }

    @Override
    public Either<Constant, String> tryApply(BinaryOperator op, Constant rhs, DiagnosticReporter reporter) {
        return Either.left(apply(op, rhs));
    }

    @Override
    public ConstantInt apply(Comparison cmp, Constant rhs) {
        throw new UnsupportedOperationException("Unsupported operation");
    }

    @Override
    public boolean isZero() {
        return false;
    }

    @Override
    public PointerType getType() {
        return new PointerType(referencedType);
    }

    @Override
    public AsmType getAsmType() {
        return AsmType.DWORD;
    }

    @Override
    public boolean isNullPointer() {
        return false;
    }
}
