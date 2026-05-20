package net.flymachine.minecraftclanguage.content.logic.compiler.frontend.c99.ast;

import net.flymachine.minecraftclanguage.content.logic.compiler.frontend.c99.ast.node.*;

public interface StatementVisitor {

    void visit(ReturnNode ret);

    void visit(ExpressionStatementNode expStmt);

    void visit(IfStatementNode ifStmt);

    void visit(GotoNode gotoStmt);

    void visit(CompoundStatementNode compoundStmt);

    void visit(BreakNode breakStmt);

    void visit(ContinueNode continueStmt);

    void visit(WhileLoopNode whileLoop);

    void visit(ForLoopNode forLoop);

    void visit(SwitchStatementNode switchStmt);

    void visit(NullStatementNode nullStmt);
}
