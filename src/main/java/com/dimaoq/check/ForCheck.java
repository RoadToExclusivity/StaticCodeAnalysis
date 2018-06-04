package com.dimaoq.check;

import com.github.javaparser.Position;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.expr.*;
import com.github.javaparser.ast.stmt.ForStmt;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;

public class ForCheck extends VoidVisitorAdapter<ForCheck.ForCheckArg> {

    public static class ForCheckArg {
        public HashSet<String> visited;
        public List<Position> result;
    }

    private class VisitCmpExpr extends VoidVisitorAdapter<ForCheckArg> {
        @Override
        public void visit(NameExpr n, ForCheckArg arg) {
            arg.visited.add(n.getNameAsString());
        }

        @Override
        public void visit(FieldAccessExpr n, ForCheckArg arg) {
            arg.visited.add(n.toString());
        }

        @Override
        public void visit(MethodCallExpr n, ForCheckArg arg) {
            n.getArguments().forEach((p) -> {
                p.accept(this, arg);
            });

            n.getScope().ifPresent((l) -> {
                l.accept(this, arg);
            });

            n.getTypeArguments().ifPresent((l) -> {
                l.forEach((v) -> {
                    v.accept(this, arg);
                });
            });
        }
    }

    private class VisitUpdateExpr extends VisitCmpExpr {
        @Override
        public void visit(AssignExpr n, ForCheckArg arg) {
            n.getTarget().accept(this, arg);
        }
    }

    @Override
    public void visit(ForStmt n, ForCheckArg arg) {
        Optional<Expression> compares = n.getCompare();
        NodeList<Expression> update = n.getUpdate();
        if (!compares.isPresent() || update.isEmpty()) {
            super.visit(n, arg);
            return;
        }
        Expression cmp = compares.get();
        HashSet<String> vars = new HashSet<>();
        arg.visited = vars;
        cmp.accept(new VisitCmpExpr(), arg);

        HashSet<String> upds = new HashSet<>();
        arg.visited = upds;
        for (Expression e : update) {
            e.accept(new VisitUpdateExpr(), arg);
        }

        boolean ok = false;
        for (String s : upds) {
            if (vars.contains(s)) {
                ok = true;
                break;
            }
        }

        if (!ok) {
            arg.result.add(n.getBegin().get());
        }

        super.visit(n, arg);
    }
}
