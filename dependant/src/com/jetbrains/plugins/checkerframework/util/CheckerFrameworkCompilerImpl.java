package com.jetbrains.plugins.checkerframework.util;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Computable;
import com.intellij.openapi.util.Key;
import com.intellij.psi.*;
import com.jetbrains.plugins.checkerframework.tools.FilteringDiagnosticCollector;
import com.jetbrains.plugins.checkerframework.tools.PsiClassJavaFileObject;
import com.jetbrains.plugins.checkerframework.tools.PsiJavaFileManager;
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
import javax.annotation.processing.Processor;
import javax.tools.*;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicReference;

import static com.sun.tools.javac.code.Symbol.ClassSymbol;

@SuppressWarnings("UseOfSystemOutOrSystemErr")
public class CheckerFrameworkCompilerImpl extends PsiTreeChangeAdapter {

    private static final Logger                  LOG           = Logger.getInstance(CheckerFrameworkCompilerImpl.class);
    private static final JavacTool               JAVA_COMPILER = JavacTool.create();
    private static final StandardJavaFileManager FILE_MANAGER  = JAVA_COMPILER.getStandardFileManager(null, null, null);

    static {
        JavaParser.setCacheParser(false);
    }

    private final FilteringDiagnosticCollector myDiagnosticCollector = new FilteringDiagnosticCollector();
    private final Set<PsiClass>                changedNames          = new HashSet<PsiClass>();
    private final Context               mySharedContext;
    private final AggregateCheckerEx    processor;
    private final ProcessingEnvironment environment;
    private final JavaCompiler          compiler;
    private final Check                 check;
    private final Todo                  todo;
    private final Names                 names;
    private final Symtab                symtab;
    private final Key<Boolean>             key     = Key.create("usedInSymtab");
    private final AtomicReference<Boolean> running = new AtomicReference<Boolean>(false);

    public CheckerFrameworkCompilerImpl(final @NotNull Project project,
                                        final @NotNull Collection<String> compileOptions,
                                        final @NotNull Collection<Class<? extends Processor>> classes) {
        System.out.println("Compiler instance creating start");
        long startTime = System.currentTimeMillis();

        mySharedContext = new Context();
        { // init context with own file manager
            mySharedContext.put(DiagnosticListener.class, myDiagnosticCollector);
            final JavaFileManager fileManager = new PsiJavaFileManager(FILE_MANAGER, project);
            mySharedContext.put(JavaFileManager.class, fileManager);
            JavacTool.processOptions(mySharedContext, fileManager, compileOptions);
        }
        todo = Todo.instance(mySharedContext);
        compiler = JavaCompiler.instance(mySharedContext);
        names = Names.instance(mySharedContext);
        symtab = Symtab.instance(mySharedContext);
        check = Check.instance(mySharedContext);
        environment = JavacProcessingEnvironment.instance(mySharedContext);

        // init processor
        processor = new AggregateCheckerEx() {
            @Override
            protected Collection<Class<? extends SourceChecker>> getSupportedCheckers() {
                final Set<Class<? extends SourceChecker>> myClasses = new HashSet<Class<? extends SourceChecker>>();
                for (Class<?> clazz : classes) {
                    myClasses.add(clazz.asSubclass(SourceChecker.class));
                }
                return myClasses;
            }
        };
        processor.setProcessingEnvironment(environment);
        processor.initChecker();
        System.out.println("Compiler instance created, " + (System.currentTimeMillis() - startTime) + "ms elapsed");
        PsiManager.getInstance(project).addPsiTreeChangeListener(this);
    }

    @SuppressWarnings("UnusedDeclaration")
    @Nullable
    public List<Diagnostic<? extends JavaFileObject>> getMessages(@NotNull final PsiClass psiClass) throws Exception {
        if (running.compareAndSet(true, true)) {
            return null;
        }
        for (final PsiClass aClass : changedNames) {
            clean(aClass);
        }
        final Callable<ClassSymbol> classSymbolCompleter = new Completer(psiClass);
        if (psiClass.getUserData(key) == null) {
            psiClass.putUserData(key, true);
            ApplicationManager.getApplication().executeOnPooledThread(classSymbolCompleter);
            return null;
        } else {
            clean(psiClass);
            final ClassSymbol classSymbol = classSymbolCompleter.call();
            long start = System.currentTimeMillis();
            System.out.println("Processing " + classSymbol + " start");
            try {
                ApplicationManager.getApplication().executeOnPooledThread(new Runnable() {
                    @Override
                    public void run() {
                        processor.typeProcess(classSymbol, Trees.instance(environment).getPath(classSymbol));
                    }
                }).get(10000, TimeUnit.MILLISECONDS);
            } catch (TimeoutException ignored) {
                System.out.println("Processing " + classSymbol + " is too long");
                return null;
            }
            System.out.println("Processing " + classSymbol + " " + (System.currentTimeMillis() - start) + "ms elapsed");
            return myDiagnosticCollector.getAndClear();
        }
    }

    public Name getFlatName(PsiClass psiClass) {
        return names.fromString(
            psiClass.getContainingClass() == null
                ? psiClass.getQualifiedName()
                : psiClass.getContainingClass().getQualifiedName() + "$" + psiClass.getName()
        );
    }

    public void clean(PsiClass psiClass) {
        final Name flatname = getFlatName(psiClass);
        symtab.classes.remove(flatname);
        check.compiled.remove(flatname);
        for (PsiClass inner : psiClass.getInnerClasses()) {
            if (psiClass.getQualifiedName() != null) {
                clean(inner);
            }
        }
    }

    public void clean(PsiJavaFile javaFile) {
        synchronized (changedNames) {
            for (PsiClass psiClass : javaFile.getClasses()) {
                if (running.compareAndSet(true, true)) {
                    changedNames.add(psiClass);
                } else {
                    clean(psiClass);
                }
            }
        }
    }

    public void clean(PsiFile file) {
        if (file instanceof PsiJavaFile) {
            clean((PsiJavaFile) file);
        }
    }

    private void clean(PsiTreeChangeEvent event) {
        clean(event.getFile());
    }

    @Override
    public void childAdded(@NotNull PsiTreeChangeEvent event) {
        clean(event);
    }

    @Override
    public void childRemoved(@NotNull PsiTreeChangeEvent event) {
        clean(event);
    }

    @Override
    public void childReplaced(@NotNull PsiTreeChangeEvent event) {
        clean(event);
    }

    @Override
    public void childMoved(@NotNull PsiTreeChangeEvent event) {
        clean(event);
    }

    @Override
    public void childrenChanged(@NotNull PsiTreeChangeEvent event) {
        clean(event);
    }

    private class Completer implements Callable<ClassSymbol> {

        private final PsiClass clazz;

        public Completer(PsiClass clazz) {
            this.clazz = clazz;
        }

        @Override
        public ClassSymbol call() throws Exception {
            long start = System.currentTimeMillis();
            try {
                running.set(true);
                final ClassSymbol classSymbol = ApplicationManager.getApplication().runReadAction(new Computable<ClassSymbol>() {
                    @Override
                    public ClassSymbol compute() {
                        return ClassReader.instance(mySharedContext).enterClass(
                            getFlatName(clazz),
                            new PsiClassJavaFileObject(clazz)
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
                return classSymbol;
            } finally {
                System.out.println(System.currentTimeMillis() - start + "ms elapsed");
                running.set(false);
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
