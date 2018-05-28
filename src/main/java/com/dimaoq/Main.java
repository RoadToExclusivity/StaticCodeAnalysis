package com.dimaoq;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.symbolsolver.JavaSymbolSolver;
import com.github.javaparser.symbolsolver.javaparsermodel.JavaParserFacade;
import com.github.javaparser.symbolsolver.model.resolution.TypeSolver;

import java.io.FileInputStream;
import java.io.IOException;

// V519
// V533
// V550
// V590...
public class Main {

    public static void main(String[] args) throws IOException {
        Visitor visitor = new Visitor("/");
        TypeSolver solver = visitor.getTypeSolver();
        CompilationUnit unit = JavaParser.parse(new FileInputStream("Test.java"));
        new JavaSymbolSolver(solver).inject(unit);

        unit.accept(new Visitor.GetMethodsVisitor(), JavaParserFacade.get(solver));
        unit.accept(new Visitor.CallVisitor(), JavaParserFacade.get(solver));
        unit.accept(new Visitor.ForCheckVisitor(), JavaParserFacade.get(solver));
    }
}
