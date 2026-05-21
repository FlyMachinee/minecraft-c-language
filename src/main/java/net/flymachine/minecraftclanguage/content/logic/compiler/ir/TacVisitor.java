package net.flymachine.minecraftclanguage.content.logic.compiler.ir;

public interface TacVisitor<T> {
    T visit(TacReturn inst);

    T visit(TacUnaryOperation inst);

    T visit(TacBinaryOperation inst);

    T visit(TacCopy inst);

    T visit(TacLabel inst);

    T visit(TacJump inst);

    T visit(TacJumpIfZero inst);

    T visit(TacJumpIfNotZero inst);

    T visit(TacJumpIfComparison inst);

    T visit(TacFunctionCall inst);

    T visit(TacSignExtend inst);

    T visit(TacTruncate inst);

    T visit(TacZeroExtend inst);
}
