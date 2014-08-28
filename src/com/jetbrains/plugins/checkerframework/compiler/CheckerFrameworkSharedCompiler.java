package com.jetbrains.plugins.checkerframework.compiler;

import com.intellij.codeInspection.ProblemDescriptor;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.progress.ProcessCanceledException;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Computable;
import com.intellij.openapi.util.Key;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiJavaFile;
import com.intellij.psi.PsiTreeChangeAdapter;
import com.jetbrains.plugins.checkerframework.service.CheckerFrameworkProblemDescriptorBuilder;
import com.jetbrains.plugins.checkerframework.tools.*;
import com.jetbrains.plugins.checkerframework.util.CheckerFrameworkModificationListener;
import com.sun.source.util.TreePath;
import com.sun.source.util.Trees;
import com.sun.tools.javac.api.JavacTool;
import com.sun.tools.javac.code.Symtab;
import com.sun.tools.javac.comp.AttrContext;
import com.sun.tools.javac.comp.Check;
import com.sun.tools.javac.comp.Env;
import com.sun.tools.javac.comp.Todo;
import com.sun.tools.javac.jvm.ClassReader;
import com.sun.tools.javac.main.JavaCompiler;
import com.sun.tools.javac.processing.JavacProcessingEnvironment;
import com.sun.tools.javac.util.Context;
import com.sun.tools.javac.util.Name;
import com.sun.tools.javac.util.Names;
import org.checkerframework.framework.source.AggregateChecker;
import org.checkerframework.framework.source.SourceChecker;
import org.checkerframework.stubparser.JavaParser;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.annotation.processing.ProcessingEnvironment;
import javax.tools.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicReference;

import static com.sun.tools.javac.code.Symbol.ClassSymbol;

@SuppressWarnings("UseOfSystemOutOrSystemErr")
public class CheckerFrameworkSharedCompiler extends PsiTreeChangeAdapter {

    @SuppressWarnings("UnusedDeclaration")
    private static final Logger                  LOG           = Logger.getInstance(CheckerFrameworkSharedCompiler.class);
    private static final JavacTool               JAVA_COMPILER = JavacTool.create();
    private static final StandardJavaFileManager FILE_MANAGER  = JAVA_COMPILER.getStandardFileManager(null, null, null);
    private static final Key<Boolean>            KEY           = Key.create("usedInSymtab");

    static {
        JavaParser.setCacheParser(false);
    }

    private final Project myProject;
    private final ReusableDiagnosticCollector myReusableDiagnosticCollector = new ReusableDiagnosticCollector();
    private final AtomicReference<Boolean>    symtabRunning                 = new AtomicReference<Boolean>(false);
    private final Collection<Class<? extends SourceChecker>> myEnabledCheckerClasses;
    private final CheckerFrameworkProblemDescriptorBuilder   descriptorBuilder;
    private final CheckerFrameworkModificationListener       myModificationListener;
    private       AggregateCheckerEx                         processor;

    private final JavaFileManager       fileManager;
    private final Context               mySharedContext;
    private final ProcessingEnvironment environment;
    private final JavaCompiler          compiler;
    private final Check                 check;
    private final Todo                  todo;
    private final Names                 names;
    private final Symtab                symtab;
    private final ClassReader           classReader;

    public CheckerFrameworkSharedCompiler(final @NotNull Project project,
                                          final @NotNull Collection<String> compileOptions,
                                          final @NotNull Collection<Class<? extends SourceChecker>> classes) {
        long startTime = System.currentTimeMillis();
        System.out.println("Compiler instance creating start");

        myProject = project;
        myEnabledCheckerClasses = classes;
        descriptorBuilder = CheckerFrameworkProblemDescriptorBuilder.getInstance(project);
        myModificationListener = CheckerFrameworkModificationListener.getInstance(project);

        fileManager = new PsiJavaFileManager(FILE_MANAGER, project);
        mySharedContext = new ThreadContext();
        { // init context with own file manager
            ThreadedTrees.preRegister(mySharedContext);
            mySharedContext.put(DiagnosticListener.class, myReusableDiagnosticCollector);
            mySharedContext.put(JavaFileManager.class, fileManager);
            JavacTool.processOptions(mySharedContext, fileManager, compileOptions);
        }
        todo = Todo.instance(mySharedContext);
        compiler = JavaCompiler.instance(mySharedContext);
        names = Names.instance(mySharedContext);
        symtab = Symtab.instance(mySharedContext);
        check = Check.instance(mySharedContext);
        environment = JavacProcessingEnvironment.instance(mySharedContext);
        classReader = ClassReader.instance(mySharedContext);

        processor = createAntInitProcessor();

        System.out.println("Compiler instance created, " + (System.currentTimeMillis() - startTime) + "ms elapsed");
    }

    private AggregateCheckerEx createAntInitProcessor() {
        AggregateCheckerEx result = new AggregateCheckerEx() {
            @Override
            protected Collection<Class<? extends SourceChecker>> getSupportedCheckers() {
                return myEnabledCheckerClasses;
            }
        };
        result.setProcessingEnvironment(environment);
        result.initChecker();
        return result;
    }

    @Nullable
    public List<ProblemDescriptor> processFile(@NotNull final PsiJavaFile psiJavaFile, final boolean isOnTheFly) {
        if (symtabRunning.compareAndSet(true, true)) {
            return null;
        }
        {   // remove & reenter changed files
            for (final PsiJavaFile javaFile : myModificationListener.getChangedFiles()) {
                clean(javaFile);
                final PsiJavaFileObject javaFileObject = new PsiJavaFileObject(javaFile);
                classReader.enterClass(
                    names.fromString(fileManager.inferBinaryName(null, javaFileObject)),
                    javaFileObject
                );
            }
        }
        final Completer classSymbolCompleter = new Completer(psiJavaFile);
        if (psiJavaFile.getUserData(KEY) != null || !isOnTheFly) {
            final List<ClassSymbol> classSymbols;
            try {
                classSymbols = classSymbolCompleter.call();
            } catch (Exception e) {
                LOG.error(e);
                return null;
            }
            final List<ProblemDescriptor> problems = new ArrayList<ProblemDescriptor>();
//            final Future future = ApplicationManager.getApplication().executeOnPooledThread();
            try {
                myReusableDiagnosticCollector.setInternal(new DiagnosticListener<JavaFileObject>() {
                    @Override
                    public void report(final Diagnostic<? extends JavaFileObject> diagnostic) {
                        final ProblemDescriptor problemDescriptor = ApplicationManager.getApplication().runReadAction(new Computable<ProblemDescriptor>() {
                            @Override
                            public ProblemDescriptor compute() {
                                return descriptorBuilder.buildProblemDescriptor(
                                    psiJavaFile,
                                    diagnostic,
                                    isOnTheFly
                                );
                            }
                        });
                        if (problemDescriptor != null) {
                            problems.add(problemDescriptor);
                        }
                    }
                });
                processor = isOnTheFly ? processor : createAntInitProcessor();
                final ProcessorRunner processorRunner = new ProcessorRunner(classSymbols);
                processorRunner.run();
//                if (isOnTheFly) {
//                    future.get(1000, TimeUnit.MILLISECONDS);
//                } else {
//                future.get();
//                }
//            } catch (InterruptedException e) {
//                LOG.error(e);
//            } catch (ExecutionException e) {
//                LOG.error(e);
            } catch (ProcessCanceledException e) {
//                future.cancel(true);
                problems.clear();
                problems.add(descriptorBuilder.buildTooLongProblem(psiJavaFile));
            } finally {
                myReusableDiagnosticCollector.setInternal(null);
            }
            return problems;
        } else {
            psiJavaFile.putUserData(KEY, true);
            ApplicationManager.getApplication().executeOnPooledThread((Runnable) classSymbolCompleter);
            return null;
        }
    }

    public Name getFlatName(PsiClass psiClass) {
        return names.fromString(
            psiClass.getContainingClass() == null
                ? psiClass.getQualifiedName()
                : psiClass.getContainingClass().getQualifiedName() + "$" + psiClass.getName()
        );
    }

    public boolean clean(PsiClass psiClass) {
        final Name flatname = getFlatName(psiClass);
        final ClassSymbol old = symtab.classes.remove(flatname);
        check.compiled.remove(flatname);
        for (PsiClass inner : psiClass.getInnerClasses()) {
            if (psiClass.getQualifiedName() != null) {
                clean(inner);
            }
        }
        return old != null;
    }

    public void clean(PsiJavaFile javaFile) {
        for (PsiClass psiClass : javaFile.getClasses()) {
            clean(psiClass);
        }
    }


    private class ProcessorRunner implements Runnable {

        private final List<ClassSymbol> classSymbols;

        private ProcessorRunner(List<ClassSymbol> classSymbols) {
            this.classSymbols = classSymbols;
        }

        @Override
        public void run() {
            for (ClassSymbol classSymbol : classSymbols) {
                long start = System.currentTimeMillis();
                try {
                    System.out.println("Processing " + classSymbol + " start");
                    TreePath treePath;
                    try {
                        treePath = Trees.instance(environment).getPath(classSymbol);
                    } catch (AssertionError e) {
                        throw (RuntimeException) e.getCause();
                    }
                    processor.typeProcess(classSymbol, treePath);
//                } catch (EnoughException e) {
//                    LOG.debug(e);
                } finally {
                    System.out.println("Processing " + classSymbol + " " + (System.currentTimeMillis() - start) + "ms elapsed");
                }
            }
        }
    }

    private class Completer implements Callable<List<ClassSymbol>>, Runnable {

        private final PsiJavaFile myPsiJavaFile;

        private Completer(PsiJavaFile psiJavaFile) {
            this.myPsiJavaFile = psiJavaFile;
        }

        @Override
        public List<ClassSymbol> call() throws Exception {
            long start = System.currentTimeMillis();

            try {
                symtabRunning.set(true);
                final ClassSymbol classSymbol = ApplicationManager.getApplication().runReadAction(new Computable<ClassSymbol>() {
                    @Override
                    public ClassSymbol compute() {
                        clean(myPsiJavaFile);
                        final PsiJavaFileObject javaFileObject = new PsiJavaFileObject(myPsiJavaFile);
                        return ClassReader.instance(mySharedContext).enterClass(
                            names.fromString(fileManager.inferBinaryName(null, javaFileObject)),
                            javaFileObject
                        );
                    }
                });
                classSymbol.complete();
                while (!todo.isEmpty()) {
                    final Env<AttrContext> current = todo.remove();
                    synchronized (todo) {
                        compiler.flow(compiler.attribute(current));
                    }
                }

                return ApplicationManager.getApplication().runReadAction(new Computable<List<ClassSymbol>>() {
                    @Override
                    public List<ClassSymbol> compute() {
                        final List<ClassSymbol> result = new ArrayList<ClassSymbol>();
                        for (final PsiClass psiClass : myPsiJavaFile.getClasses()) {
                            final Name name = getFlatName(psiClass);
                            result.add(symtab.classes.get(name));
                        }
                        return result;
                    }
                });
            } finally {
                System.out.println(System.currentTimeMillis() - start + "ms elapsed");
                symtabRunning.set(false);
            }
        }

        @Override
        public void run() {
            try {
                call();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    private abstract static class AggregateCheckerEx extends AggregateChecker {

        @Override
        public void setProcessingEnvironment(ProcessingEnvironment env) {
            super.setProcessingEnvironment(env);
        }
    }
}
