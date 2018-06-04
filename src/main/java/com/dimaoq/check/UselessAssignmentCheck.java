package com.dimaoq.check;

import com.github.javaparser.Position;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.expr.AssignExpr;
import com.github.javaparser.ast.expr.FieldAccessExpr;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

public class UselessAssignmentCheck extends VoidVisitorAdapter<ArrayList<Position>> {

    public static class DoubleAssignmentArg {
        public HashSet<String> wasAssigned;
        public HashSet<String> visited;
        public List<Position> positions;
    }

    public static class AssignmentVisitor extends VoidVisitorAdapter<DoubleAssignmentArg> {

        @Override
        public void visit(NameExpr n, DoubleAssignmentArg arg) {
            arg.visited.add(n.getNameAsString());
        }


        @Override
        public void visit(FieldAccessExpr n, DoubleAssignmentArg arg) {
            arg.visited.add(n.toString());
        }

        @Override
        public void visit(VariableDeclarator n, DoubleAssignmentArg arg) {
            if (n.getInitializer().isPresent()) {
                arg.wasAssigned.add(n.getNameAsString());
                n.getInitializer().get().accept(this, arg);
            }
        }

        @Override
        public void visit(AssignExpr n, DoubleAssignmentArg arg) {
            n.getValue().accept(this, arg);

            if (n.getTarget().isNameExpr()) {
                String name = n.getTarget().asNameExpr().getNameAsString();
                if (arg.wasAssigned.contains(name)) {
                    if (arg.visited.contains(name)) {
                        arg.visited.remove(name);
                    } else {
                        arg.positions.add(n.getBegin().get());
                    }
                } else {
                    arg.wasAssigned.add(name);
                }
            } else if (n.getTarget().isFieldAccessExpr()) {
                String name = n.getTarget().asFieldAccessExpr().toString();
                if (arg.wasAssigned.contains(name)) {
                    if (arg.visited.contains(name)) {
                        arg.visited.remove(name);
                    } else {
                        arg.positions.add(n.getBegin().get());
                    }
                } else {
                    arg.wasAssigned.add(name);
                }
            }
        }
    }

    @Override
    public void visit(BlockStmt n, ArrayList<Position> arg) {
        DoubleAssignmentArg argd = new DoubleAssignmentArg();
        argd.wasAssigned = new HashSet<>();
        argd.visited = new HashSet<>();
        argd.positions = new ArrayList<>();
        n.accept(new AssignmentVisitor(), argd);

        arg.addAll(argd.positions);
        super.visit(n, arg);
    }
}
