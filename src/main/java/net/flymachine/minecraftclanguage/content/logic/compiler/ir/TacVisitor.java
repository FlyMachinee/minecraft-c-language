package net.flymachine.minecraftclanguage.content.logic.compiler.ir;

public interface TacVisitor<T> {
    T visitReturn(TacReturn inst);

    T visitUnaryOperation(TacUnaryOperation inst);

    T visitBinaryOperation(TacBinaryOperation inst);

    T visitCopy(TacCopy inst);

    T visitLabel(TacLabel inst);

    T visitJump(TacJump inst);

    T visitJumpIfZero(TacJumpIfZero inst);

    T visitJumpIfNotZero(TacJumpIfNotZero inst);

    T visitJumpIfComparison(TacJumpIfComparison inst);
}
