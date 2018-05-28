package com.dimaoq;

import com.github.javaparser.Position;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.*;
import com.github.javaparser.ast.stmt.ForStmt;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;
import com.github.javaparser.resolution.declarations.ResolvedMethodDeclaration;
import com.github.javaparser.symbolsolver.javaparsermodel.JavaParserFacade;
import com.github.javaparser.symbolsolver.model.resolution.SymbolReference;
import com.github.javaparser.symbolsolver.model.resolution.TypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.CombinedTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.JavaParserTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.ReflectionTypeSolver;

import java.io.File;
import java.util.HashSet;
import java.util.Optional;

public class Visitor {
    private CombinedTypeSolver typeSolver;

    public static class GetMethodsVisitor extends VoidVisitorAdapter<JavaParserFacade> {
        @Override
        public void visit(MethodDeclaration n, JavaParserFacade facade) {
            super.visit(n, facade);
            ResolvedMethodDeclaration decl =  n.resolve();
            System.out.println(String.format("Ret type = %s, name = %s, argCount = %d",
                    decl.getReturnType().describe(), decl.getQualifiedName(), decl.getNumberOfParams()));
        }
    }

    public static class CallVisitor extends VoidVisitorAdapter<JavaParserFacade>{
        @Override
        public void visit(MethodCallExpr n, JavaParserFacade facade) {
            super.visit(n, facade);
            SymbolReference<ResolvedMethodDeclaration> fsolve = facade.solve(n);
            if (fsolve.isSolved())
                System.out.println(String.format("Call of method %s", fsolve.getCorrespondingDeclaration().getQualifiedName()));
        }
    }

    private static class VisitExpr extends VoidVisitorAdapter<HashSet<String>> {
        @Override
        public void visit(NameExpr n, HashSet<String> arg) {
            super.visit(n, arg);

            arg.add(n.getNameAsString());
        }
    }

    private static class VisitUpdateExpr extends VoidVisitorAdapter<HashSet<String>> {
        @Override
        public void visit(UnaryExpr n, HashSet<String> arg) {
            super.visit(n, arg);

            UnaryExpr.Operator op = n.getOperator();
            if (op == UnaryExpr.Operator.POSTFIX_INCREMENT ||
                    op == UnaryExpr.Operator.POSTFIX_DECREMENT ||
                    op == UnaryExpr.Operator.PREFIX_INCREMENT ||
                    op == UnaryExpr.Operator.PREFIX_DECREMENT) {
                n.getExpression().accept(new VisitExpr(), arg);
            }
        }

        @Override
        public void visit(AssignExpr n, HashSet<String> arg) {
            super.visit(n, arg);

            n.getTarget().accept(new VisitExpr(), arg);
        }
    }

    public static class ForCheckVisitor extends VoidVisitorAdapter<JavaParserFacade> {
        @Override
        public void visit(ForStmt n, JavaParserFacade facade) {
            super.visit(n, facade);
            Optional<Expression> init = n.getCompare();
            if (!init.isPresent()) {
                return;
            }
            Expression cmp = init.get();
            HashSet<String> vars = new HashSet<>();
            cmp.accept(new VisitExpr(), vars);
            System.out.println(String.format("VARS = %s", vars.toString()));
            NodeList<Expression> update = n.getUpdate();

            HashSet<String> upds = new HashSet<>();
            for (Expression e : update) {
                e.accept(new VisitUpdateExpr(), upds);
            }

            System.out.println(String.format("UPDS = %s", upds.toString()));

            boolean ok = false;
            for (String s : upds) {
                if (vars.contains(s)) {
                    ok = true;
                    break;
                }
            }

            if (!ok) {
                Position pos = n.getBegin().get();
                System.out.println(String.format("Probably wrong update expression in for statement at line %d", pos.line));
            }
        }
    }

    Visitor(String source) {
        typeSolver = new CombinedTypeSolver();
        typeSolver.add(new ReflectionTypeSolver());
        typeSolver.add(new JavaParserTypeSolver(new File(source)));
    }

    public TypeSolver getTypeSolver() {
        return typeSolver;
    }
}
