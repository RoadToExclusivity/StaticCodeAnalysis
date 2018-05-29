package com.dimaoq;

import com.github.javaparser.ParseResult;
import com.github.javaparser.ParserConfiguration;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.symbolsolver.JavaSymbolSolver;
import com.github.javaparser.symbolsolver.javaparsermodel.JavaParserFacade;
import com.github.javaparser.symbolsolver.model.resolution.TypeSolver;
import com.github.javaparser.utils.SourceRoot;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

// V519
// V533
// V550
// V590...

public class Main {

    private static void write(String s) {
        System.out.println(s);
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

        ParserConfiguration configuration = new ParserConfiguration().setSymbolResolver(new JavaSymbolSolver(solver));
        SourceRoot root = new SourceRoot(new File(args[0]).toPath(), configuration);
        List<ParseResult<CompilationUnit>> results = root.tryToParse();

        for (ParseResult<CompilationUnit> res : results) {
            if (res.isSuccessful() && res.getResult().isPresent()) {
                CompilationUnit unit = res.getResult().get();
//                write("Parsing: " + unit.getStorage().get().getPath().toString());
                arg.unit = unit;
                unit.accept(new Visitor.ForCheckVisitor(), arg);
//                write("Parsed success: " + unit.getStorage().get().getPath().toString());
            }
        }
    }
}
