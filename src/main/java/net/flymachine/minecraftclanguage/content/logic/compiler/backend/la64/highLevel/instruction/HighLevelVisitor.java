package net.flymachine.minecraftclanguage.content.logic.compiler.backend.la64.highLevel.instruction;

public interface HighLevelVisitor<T> {
    T visitMove(Move inst);

    T visitRet(Ret inst);

    T visitUnary(Unary inst);

    T visitBinary(Binary inst);

    T visitAddSi12(AddSi12 inst);

    T visitLabel(Label inst);

    T visitBranch(Branch inst);

    T visitBranchIfZero(BranchIfZero inst);

    T visitBranchIfNotZero(BranchIfNotZero inst);

    T visitBranchIfComparison(BranchIfComparison inst);
}
