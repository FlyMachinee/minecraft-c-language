package net.flymachine.minecraftclanguage.content.logic.compiler.common.constant;

import net.flymachine.minecraftclanguage.content.logic.compiler.backend.la64.highLevel.AsmType;
import net.flymachine.minecraftclanguage.content.logic.compiler.common.BinaryOperator;
import net.flymachine.minecraftclanguage.content.logic.compiler.common.Comparison;
import net.flymachine.minecraftclanguage.content.logic.compiler.common.UnaryOperator;
import net.flymachine.minecraftclanguage.content.logic.compiler.common.staticInit.StaticInit;
import net.flymachine.minecraftclanguage.content.logic.compiler.common.staticInit.UnsignedLongInit;
import net.flymachine.minecraftclanguage.content.logic.compiler.common.type.PointerType;
import net.flymachine.minecraftclanguage.content.logic.compiler.common.type.Type;
import org.jetbrains.annotations.NotNull;

public record ConstantPointer(long value, Type referencedType) implements Constant {

    @Override
    public @NotNull String toString() {
        return "(" + (new PointerType(referencedType)) + ")" + value + "ULL";
    }

    @Override
    public ConstantInt toInt() {
        return toUnsignedLong().toInt();
    }

    @Override
    public ConstantLong toLong() {
        return toUnsignedLong().toLong();
    }

    @Override
    public ConstantUnsignedInt toUnsignedInt() {
        return toUnsignedLong().toUnsignedInt();
    }

    @Override
    public ConstantUnsignedLong toUnsignedLong() {
        return new ConstantUnsignedLong(value);
    }

    @Override
    public ConstantDouble toDouble() {
        throw new UnsupportedOperationException("Invalid cast");
    }

    @Override
    public ConstantPointer toPointer(Type referencedType) {
        return new ConstantPointer(value, referencedType);
    }

    @Override
    public StaticInit toStaticInit() {
        return new UnsignedLongInit(value);
    }

    @Override
    public ConstantInt apply(UnaryOperator op) {
        return switch (op) {
            case NEGATE -> throw new UnsupportedOperationException("Not arithmetic type");
            case COMPLEMENT -> throw new UnsupportedOperationException("Not integer type");
            case NOT -> new ConstantInt(value == 0 ? 1 : 0);
        };
    }

    @Override
    public Constant apply(BinaryOperator op, Constant rhs) {
        PointerType lhsType = getType();
        Type rhsType = rhs.getType();

        return switch (op) {
            case ADD, SUBTRACT, MULTIPLY, DIVIDE, LESS_THAN, LESS_OR_EQUAL, GREATER_THAN, GREATER_OR_EQUAL, LEFT_SHIFT,
                 RIGHT_SHIFT, MODULO, BITWISE_AND, BITWISE_OR, BITWISE_XOR ->
                throw new UnsupportedOperationException("Unsupported operation");
            case LOGICAL_AND, LOGICAL_OR -> {
                if (rhsType.isScalar()) {
                    if (op == BinaryOperator.LOGICAL_AND) {
                        yield new ConstantInt(this.isZero() || rhs.isZero() ? 0 : 1);
                    } else {
                        yield new ConstantInt(this.isZero() && rhs.isZero() ? 0 : 1);
                    }
                }
                throw new UnsupportedOperationException("Unsupported operation");
            }
            case EQUAL, NOT_EQUAL -> {
                if (rhsType instanceof PointerType rhsPtrType) {
                    if (!lhsType.referencedType().isCompatible(rhsPtrType.referencedType())) {
                        throw new UnsupportedOperationException("Only allow compatible pointers");
                    }
                    if (op == BinaryOperator.EQUAL) {
                        yield new ConstantInt(this.value == ((ConstantPointer) rhs).value ? 1 : 0);
                    } else {
                        yield new ConstantInt(this.value != ((ConstantPointer) rhs).value ? 1 : 0);
                    }
                }
                throw new UnsupportedOperationException("Unsupported operation");
            }
        };
    }

    @Override
    public ConstantInt apply(Comparison cmp, Constant rhs) {
        return (ConstantInt) apply(cmp.toBinaryOperator(), rhs);
    }

    @Override
    public boolean isZero() {
        return value == 0;
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
        return value == 0 && referencedType.isVoid();
    }
}
