package com.dimaoq;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.symbolsolver.javaparsermodel.JavaParserFacade;

import java.util.HashSet;

public class VisitorArg {
    public JavaParserFacade facade;
    public CompilationUnit unit;
    public HashSet<String> set;
}
