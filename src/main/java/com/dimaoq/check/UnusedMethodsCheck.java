package com.dimaoq.check;

import com.github.javaparser.Position;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;
import com.github.javaparser.resolution.declarations.ResolvedMethodDeclaration;
import com.github.javaparser.symbolsolver.javaparsermodel.JavaParserFacade;
import com.github.javaparser.symbolsolver.model.resolution.SymbolReference;

import java.nio.file.Path;
import java.util.HashMap;

public class UnusedMethodsCheck extends VoidVisitorAdapter<UnusedMethodsCheck.UnusedMethodsArg> {
    private static void writeErr(CompilationUnit unit, Position pos, String s) {
        System.err.println(String.format("%s, line %d: %s",
                unit.getStorage().get().getPath().toAbsolutePath().toString(), pos.line, s));
    }

    public static class DeclarationInfo {
        public Path file;
        public Integer line;
        public ResolvedMethodDeclaration decl;
    }

    public static class UnusedMethodsArg {
        public JavaParserFacade facade;
        public CompilationUnit unit;
        public HashMap<Integer, DeclarationInfo> declarations;
        public HashMap<Integer, ResolvedMethodDeclaration> usedCalls;
    }

    public static int getHashForDeclaration(ResolvedMethodDeclaration decl) {
        StringBuilder sb = new StringBuilder();
        sb.append(decl.getReturnType().describe());
        sb.append(decl.getQualifiedName());
        sb.append(decl.getNumberOfParams());
        for (int i = 0; i < decl.getNumberOfParams(); ++i) {
            sb.append(decl.getParam(i).describeType());
        }

        return sb.toString().hashCode();
    }

    @Override
    public void visit(MethodDeclaration n, UnusedMethodsArg arg) {
        super.visit(n, arg);
        if (n.getAnnotationByName("Override").isPresent()) {
            return;
        }

        try {
            ResolvedMethodDeclaration decl =  n.resolve();
            if (decl.getReturnType().isVoid() && n.getNameAsString().equals("main")) {
                return;
            }

            DeclarationInfo info = new DeclarationInfo();
            info.decl = decl;
            info.line = n.getBegin().get().line;
            info.file = arg.unit.getStorage().get().getPath();
            arg.declarations.put(getHashForDeclaration(decl), info);
        } catch (Exception e) {
//                writeErr(arg.unit, n.getBegin().get(),
//                        String.format("failed while resolving method declaration %s: %s", n.getDeclarationAsString(), e.getLocalizedMessage()));
        }
    }

    @Override
    public void visit(MethodCallExpr n, UnusedMethodsArg arg) {
        super.visit(n, arg);
        try {
            SymbolReference<ResolvedMethodDeclaration> fsolve = arg.facade.solve(n);
            if (fsolve.isSolved()) {
                ResolvedMethodDeclaration decl = fsolve.getCorrespondingDeclaration();
                arg.usedCalls.put(getHashForDeclaration(decl), decl);
            } else {
                writeErr(arg.unit, n.getBegin().get(),
                        String.format("unresolved method usage: %s", n.getNameAsString()));
            }
        } catch (Exception e) {
//                writeErr(arg.unit, n.getBegin().get(),
//                        String.format("failed while resolving method usage %s: %s", n.getNameAsString(), e.getLocalizedMessage()));
        }

    }
}
