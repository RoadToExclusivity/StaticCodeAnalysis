package com.dimaoq;

import com.github.javaparser.ParseResult;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.resolution.declarations.ResolvedMethodDeclaration;
import com.github.javaparser.symbolsolver.javaparsermodel.JavaParserFacade;
import com.github.javaparser.symbolsolver.model.resolution.TypeSolver;
import com.github.javaparser.utils.CollectionContext;
import com.github.javaparser.utils.ProjectRoot;
import com.github.javaparser.utils.SourceRoot;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

// V519
// V533
// V550
// V590...

public class Main {
    private static class UnusedResultPair implements Comparable<UnusedResultPair> {
        public String path;
        public Visitor.DeclarationInfo info;

        @Override
        public int compareTo(UnusedResultPair that) {
            return this.path.compareTo(that.path);
        }
    }

    private static void write(String s) {
        System.out.println(s);
    }

    private static void writeErr(String s) {
        System.err.println(s);
    }

    public static void main(String[] args) throws IOException {
        if (args.length < 1) {
            write("Usage: *.jar projectSourceRoot [dependencyFile]");
            write("projectSourceRoot = ../src/");
            write("Dependency file - file with additional paths to jar files to include, 1 dependency per line");
            return;
        }

        String root = args[0];
        ArrayList<String> depPaths = new ArrayList<>();
        if (args.length > 1) {
            try (BufferedReader buf = new BufferedReader(new FileReader(args[1]))) {
                String line = buf.readLine();
                while (line != null) {
                    if (line != "") {
                        depPaths.add(line);
                    }
                    line = buf.readLine();
                }
                write("Dependency paths read from " + args[1]);
            } catch (Exception e) {
                writeErr("Error while reading dependency file " + args[1]);
            }
        } else {
            write("No dependency file provided, ok");
        }

        SolverStrategy strategy = new SolverStrategy(depPaths);
        ProjectRoot project = new CollectionContext(strategy).collect(new File(root).toPath());

        TypeSolver solver = strategy.typeSolver;
        JavaParserFacade facade = JavaParserFacade.get(solver);

        Visitor.UnusedMethodsArg unused = new Visitor.UnusedMethodsArg();

        HashMap<Integer, Visitor.DeclarationInfo> decl = new HashMap<>();
        HashMap<Integer, ResolvedMethodDeclaration> used = new HashMap<>();

        unused.facade = facade;
        unused.usedCalls = used;
        unused.declarations = decl;

        for (SourceRoot sourceRoot : project.getSourceRoots()) {
            List<ParseResult<CompilationUnit>> parsedUnits = sourceRoot.tryToParse();
            write(String.format("Parsing source project: %s, %d files",
                                sourceRoot.getRoot().toAbsolutePath().toString(), parsedUnits.size()));

            for (ParseResult<CompilationUnit> res : parsedUnits) {
                if (res.isSuccessful() && res.getResult().isPresent()) {
                    CompilationUnit unit = res.getResult().get();
    //                write("Parsing: " + unit.getStorage().get().getPath().toString());
                    //arg.unit = unit;
                    unused.unit = unit;
                    //unit.accept(new Visitor.ForCheckVisitor(), arg);

                    unit.accept(new Visitor.UnusedMethodsFinder(), unused);

    //                write("Parsed success: " + unit.getStorage().get().getPath().toString());
                }
            }
        }

        List<UnusedResultPair> unusedResults = new ArrayList<>();
        for (Visitor.DeclarationInfo info : decl.values()) {
            ResolvedMethodDeclaration declaration = info.decl;
            if (!used.containsKey(Visitor.getHashForDeclaration(declaration))) {
                UnusedResultPair pair = new UnusedResultPair();
                pair.path = info.file.toAbsolutePath().toString();
                pair.info = info;
                unusedResults.add(pair);
            }
        }

        write(String.format("\nUnused methods check: %d found", unusedResults.size()));
        Collections.sort(unusedResults);
        for (UnusedResultPair pair : unusedResults) {
            Visitor.DeclarationInfo info = pair.info;
            ResolvedMethodDeclaration declaration = pair.info.decl;
            write(String.format("%s, line %d: unused method \"%s %s(...)\"", pair.path, info.line,
                                declaration.getReturnType().describe(), declaration.getName()));
        }
//        Visitor visitor = new Visitor(depPaths);
//        TypeSolver solver = visitor.getTypeSolver();
//        JavaParserFacade facade = JavaParserFacade.get(solver);
//
//        VisitorArg arg = new VisitorArg();
//        arg.facade = facade;
//        arg.set = null;
//
//        HashMap<Integer, Visitor.DeclarationInfo> decl = new HashMap<>();
//        HashMap<Integer, ResolvedMethodDeclaration> used = new HashMap<>();
//
//        Visitor.UnusedMethodsArg unused = new Visitor.UnusedMethodsArg();
//        unused.facade = facade;
//        unused.usedCalls = used;
//        unused.declarations = decl;
//
//        ParserConfiguration configuration = new ParserConfiguration().setSymbolResolver(new JavaSymbolSolver(solver));
//        SourceRoot root = new SourceRoot(new File(args[0]).toPath(), configuration);
//        List<ParseResult<CompilationUnit>> results = root.tryToParse();
//
//        for (ParseResult<CompilationUnit> res : results) {
//            if (res.isSuccessful() && res.getResult().isPresent()) {
//                CompilationUnit unit = res.getResult().get();
////                write("Parsing: " + unit.getStorage().get().getPath().toString());
//                arg.unit = unit;
//                unused.unit = unit;
//                //unit.accept(new Visitor.ForCheckVisitor(), arg);
//
//                unit.accept(new Visitor.UnusedMethodsFinder(), unused);
//
////                write("Parsed success: " + unit.getStorage().get().getPath().toString());
//            }
//        }
//
//        for (Visitor.DeclarationInfo info : decl.values()) {
//            ResolvedMethodDeclaration declaration = info.decl;
//            if (!used.containsKey(Visitor.getHashForDeclaration(declaration))) {
//                write(String.format("%s, line %d: unused function %s %s", info.file, info.line,
//                        declaration.getReturnType().describe(), declaration.getName()));
//            }
//        }
    }
}
