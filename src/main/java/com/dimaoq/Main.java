package com.dimaoq;

import com.github.javaparser.ParseResult;
import com.github.javaparser.ParserConfiguration;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.resolution.declarations.ResolvedMethodDeclaration;
import com.github.javaparser.symbolsolver.JavaSymbolSolver;
import com.github.javaparser.symbolsolver.javaparsermodel.JavaParserFacade;
import com.github.javaparser.symbolsolver.model.resolution.TypeSolver;
import com.github.javaparser.utils.CollectionContext;
import com.github.javaparser.utils.Pair;
import com.github.javaparser.utils.ProjectRoot;
import com.github.javaparser.utils.SourceRoot;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

// V519
// V533
// V550
// V590...

public class Main {

    private static void write(String s) {
        System.out.println(s);
    }

    private static void writeErr(String s) {
        System.err.println(s);
    }

    public static void main(String[] args) throws IOException {
        if (args.length < 1) {
            write("No args provided, exit");
            return;
        }

        ArrayList<String> depPaths = new ArrayList<>();
        if (args.length > 1) {
            try {
                BufferedReader buf = new BufferedReader(new FileReader(args[1]));
                String line = buf.readLine();
                while (line != null) {
                    depPaths.add(line);
                    line = buf.readLine();
                }
                write("Dependency paths read from " + args[1]);
            } catch (Exception e) {
                System.err.println("Error while reading file %s" + args[1]);
            }
        } else {
            write("No dependency paths provided");
        }

        Visitor visitor = new Visitor(depPaths);
        TypeSolver solver = visitor.getTypeSolver();
        JavaParserFacade facade = JavaParserFacade.get(solver);

        VisitorArg arg = new VisitorArg();
        arg.facade = facade;
        arg.set = null;

        HashMap<Integer, Visitor.DeclarationInfo> decl = new HashMap<>();
        HashMap<Integer, ResolvedMethodDeclaration> used = new HashMap<>();

        Visitor.UnusedMethodsArg unused = new Visitor.UnusedMethodsArg();
        unused.facade = facade;
        unused.usedCalls = used;
        unused.declarations = decl;

        ParserConfiguration configuration = new ParserConfiguration().setSymbolResolver(new JavaSymbolSolver(solver));
        SourceRoot root = new SourceRoot(new File(args[0]).toPath(), configuration);
        List<ParseResult<CompilationUnit>> results = root.tryToParse();

        for (ParseResult<CompilationUnit> res : results) {
            if (res.isSuccessful() && res.getResult().isPresent()) {
                CompilationUnit unit = res.getResult().get();
//                write("Parsing: " + unit.getStorage().get().getPath().toString());
                arg.unit = unit;
                unused.unit = unit;
                //unit.accept(new Visitor.ForCheckVisitor(), arg);

                unit.accept(new Visitor.UnusedMethodsFinder(), unused);

//                write("Parsed success: " + unit.getStorage().get().getPath().toString());
            }
        }

        for (Visitor.DeclarationInfo info : decl.values()) {
            ResolvedMethodDeclaration declaration = info.decl;
            if (!used.containsKey(Visitor.getHashForDeclaration(declaration))) {
                write(String.format("%s, line %d: unused function %s %s", info.file, info.line,
                        declaration.getReturnType().describe(), declaration.getName()));
            }
        }
    }
}
