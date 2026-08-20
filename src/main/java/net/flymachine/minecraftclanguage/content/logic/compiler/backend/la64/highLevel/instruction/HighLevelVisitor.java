package net.flymachine.minecraftclanguage.content.logic.compiler.backend.la64.highLevel.instruction;

public interface HighLevelVisitor<T> {
    T visit(Move inst);

    T visit(Ret inst);

    T visit(Binary inst);

    T visit(AddSi12 inst);

    T visit(Label inst);

    T visit(Branch inst);

    T visit(BranchIfZero inst);

    T visit(BranchIfNotZero inst);

    T visit(BranchIfComparison inst);

    T visit(Call inst);

    T visit(Compare inst);

    T visit(DivOrMod inst);

    T visit(BitwiseShift inst);

    T visit(Bitwise inst);

    T visit(BstrpickZeroExtend inst);

    T visit(AddSignExtend inst);

    T visit(DoubleFromInt inst);

    T visit(DoubleToIntRoundZero inst);

    T visit(CompareDouble inst);

    T visit(GetCC inst);

    T visit(BranchIfCCZero inst);

    T visit(BranchIfCCNotZero inst);

    T visit(DoubleNegate inst);

    T visit(LoadAddress inst);

    T visit(Load inst);

    T visit(Store inst);
}
