package com.dimaoq;

import com.dimaoq.check.ForCheck;
import com.dimaoq.check.UnusedMethodsCheck;
import com.dimaoq.check.UselessAssignmentCheck;
import com.github.javaparser.ParseResult;
import com.github.javaparser.Position;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.resolution.declarations.ResolvedMethodDeclaration;
import com.github.javaparser.symbolsolver.javaparsermodel.JavaParserFacade;
import com.github.javaparser.symbolsolver.model.resolution.TypeSolver;
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

public class Main {
    private static class UnusedResultPair implements Comparable<UnusedResultPair> {
        String path;
        UnusedMethodsCheck.DeclarationInfo info;

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
            write("Dependency file - file with additional paths to jar files to include, 1 dependency per line");
            return;
        }

        String root = args[0];
        ArrayList<String> depPaths = new ArrayList<>();
        if (args.length > 1) {
            try (BufferedReader buf = new BufferedReader(new FileReader(args[1]))) {
                String line = buf.readLine();
                while (line != null) {
                    if (!line.equals("")) {
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
        ProjectRoot project = strategy.collect(new File(root).toPath());

        TypeSolver solver = strategy.typeSolver;
        JavaParserFacade facade = JavaParserFacade.get(solver);

        ForCheck.ForCheckArg forArg = new ForCheck.ForCheckArg();
        UnusedMethodsCheck.UnusedMethodsArg unused = new UnusedMethodsCheck.UnusedMethodsArg();

        HashMap<Integer, UnusedMethodsCheck.DeclarationInfo> decl = new HashMap<>();
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
                    String path = unit.getStorage().get().getPath().toAbsolutePath().toString();

                    unused.unit = unit;

                    forArg.visited = null;
                    forArg.result = new ArrayList<>();

                    ArrayList<Position> errors = new ArrayList<>();

                    unit.accept(new ForCheck(), forArg);
                    unit.accept(new UnusedMethodsCheck(), unused);
                    unit.accept(new UselessAssignmentCheck(), errors);

                    if (!forArg.result.isEmpty()) {
                        write("\nFound probably wrong update expressions in for-cycle: ");
                        for (Position p : forArg.result) {
                            write(String.format("%s, line %d: probably wrong update expression inside for statement",
                                                path, p.line));
                        }
                    }

                    if (!errors.isEmpty()) {
                        write("\nFound suspicious assignments: ");
                        for (Position p : errors) {
                            write(String.format("%s, line %d: second assignment without usage of target",
                                                path, p.line));
                        }
                    }
                }
            }
        }

        List<UnusedResultPair> unusedResults = new ArrayList<>();
        for (UnusedMethodsCheck.DeclarationInfo info : decl.values()) {
            ResolvedMethodDeclaration declaration = info.decl;
            if (!used.containsKey(UnusedMethodsCheck.getHashForDeclaration(declaration))) {
                UnusedResultPair pair = new UnusedResultPair();
                pair.path = info.file.toAbsolutePath().toString();
                pair.info = info;
                unusedResults.add(pair);
            }
        }

        Collections.sort(unusedResults);
        write(String.format("\nUnused methods check: %d found", unusedResults.size()));
        for (UnusedResultPair pair : unusedResults) {
            UnusedMethodsCheck.DeclarationInfo info = pair.info;
            ResolvedMethodDeclaration declaration = pair.info.decl;
            write(String.format("%s, line %d: unused method \"%s %s(...)\"", pair.path, info.line,
                                declaration.getReturnType().describe(), declaration.getName()));
        }
    }
}
