package com.dimaoq;

import com.github.javaparser.Position;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.*;
import com.github.javaparser.ast.stmt.ForStmt;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;
import com.github.javaparser.resolution.declarations.ResolvedFieldDeclaration;
import com.github.javaparser.resolution.declarations.ResolvedMethodDeclaration;
import com.github.javaparser.resolution.types.ResolvedType;
import com.github.javaparser.symbolsolver.javaparsermodel.JavaParserFacade;
import com.github.javaparser.symbolsolver.model.resolution.SymbolReference;
import com.github.javaparser.symbolsolver.model.resolution.TypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.CombinedTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.JavaParserTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.ReflectionTypeSolver;

import java.io.File;
import java.util.ArrayList;
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
            if (fsolve.isSolved()) {
                System.out.println(String.format("Call of method %s", fsolve.getCorrespondingDeclaration().getQualifiedName()));
            }
        }
    }

    private static class VisitExpr extends VoidVisitorAdapter<VisitorArg> {
        private void checkExpr(Expression e, VisitorArg arg) {
            if (e.isUnaryExpr()) {
                visit(e.asUnaryExpr(), arg);
                return;
            }

            if (e.isBinaryExpr()) {
                visit(e.asBinaryExpr(), arg);
                return;
            }

            if (e.isNameExpr()) {
                arg.set.add(e.asNameExpr().getNameAsString());
                return;
            }

            if (e.isFieldAccessExpr()) {
                arg.set.add(e.asFieldAccessExpr().toString());
                return;
            }

            if (e.isArrayAccessExpr()) {
                //TODO
                super.visit(e.asArrayAccessExpr(), arg);
                return;
            }
        }

        @Override
        public void visit(UnaryExpr n, VisitorArg arg) {
            Expression e = n.getExpression();
            checkExpr(e, arg);
        }

        @Override
        public void visit(BinaryExpr n, VisitorArg arg) {
            Expression left = n.getLeft();
            Expression right = n.getRight();

            checkExpr(left, arg);
            checkExpr(right, arg);
        }

//        @Override
//        public void visit(FieldAccessExpr n, VisitorArg arg) {
//            super.visit(n, arg);
//            SymbolReference<ResolvedFieldDeclaration> ref = arg.facade.solve(n);
//            System.out.println("Solving " + n.toString());
//            if (ref.isSolved()) {
//                System.out.println("Field = " + ref.getCorrespondingDeclaration());
//            }
//            else {
//                System.out.println("Not resolved");
//            }
//        }

//        @Override
//        public void visit(NameExpr n, VisitorArg arg) {
//            super.visit(n, arg);
//            try{
//                SymbolReference ref = arg.facade.solve(n);
//                if (ref.isSolved()) {
//                    arg.set.add(ref.getCorrespondingDeclaration().getName());
//                } else {
//                    System.err.println(String.format("Unresolved reference %s", n.getName()));
//                }
//            } catch (Exception e) {
//                System.err.println(String.format("%s, line %d: Unresolved reference: %s",
//                        arg.unit.getStorage().get().getPath().toString(), n.getBegin().get().line, n.getNameAsString()));
//            }
//        }

//        @Override
//        public void visit(FieldAccessExpr n, VisitorArg arg) {
//        }
    }

    private static class VisitUpdateExpr extends VoidVisitorAdapter<VisitorArg> {
        @Override
        public void visit(UnaryExpr n, VisitorArg arg) {
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
        public void visit(AssignExpr n, VisitorArg arg) {
            super.visit(n, arg);

            n.getTarget().accept(new VisitExpr(), arg);
        }
    }

    public static class ForCheckVisitor extends VoidVisitorAdapter<VisitorArg> {
        @Override
        public void visit(ForStmt n, VisitorArg arg) {
            Optional<Expression> compares = n.getCompare();
            NodeList<Expression> update = n.getUpdate();
            if (!compares.isPresent() || update.isEmpty()) {
                super.visit(n, arg);
                return;
            }
            Expression cmp = compares.get();
            HashSet<String> vars = new HashSet<>();
            arg.set = vars;
            cmp.accept(new VisitExpr(), arg);
            System.out.println(String.format("VARS = %s", vars.toString()));

            HashSet<String> upds = new HashSet<>();
            arg.set = upds;
            for (Expression e : update) {
                e.accept(new VisitUpdateExpr(), arg);
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
                System.out.println(String.format("%s, line %d: probably wrong update expression inside for statement",
                                    arg.unit.getStorage().get().getPath().toString(), pos.line));
            }

            super.visit(n, arg);
        }
    }

    Visitor(ArrayList<String> sources) {
        typeSolver = new CombinedTypeSolver();
        typeSolver.add(new ReflectionTypeSolver());
        for (String source : sources) {
            typeSolver.add(new JavaParserTypeSolver(new File(source)));
        }
    }

    public TypeSolver getTypeSolver() {
        return typeSolver;
    }
}
