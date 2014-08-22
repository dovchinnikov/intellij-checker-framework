package com.jetbrains.plugins.checkerframework.util;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.progress.ProcessCanceledException;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiClass;
import com.jetbrains.plugins.checkerframework.tools.FilteringDiagnosticCollector;
import com.jetbrains.plugins.checkerframework.tools.PsiClassJavaFileObject;
import com.jetbrains.plugins.checkerframework.tools.PsiJavaFileManager;
import com.sun.source.util.TreePath;
import com.sun.source.util.Trees;
import com.sun.tools.javac.api.JavacTool;
import com.sun.tools.javac.code.Symbol;
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

public class CheckerFrameworkCompilerImpl {

    private static final Logger                  LOG           = Logger.getInstance(CheckerFrameworkCompilerImpl.class);
    private static final JavacTool               JAVA_COMPILER = JavacTool.create();
    private static final StandardJavaFileManager FILE_MANAGER  = JAVA_COMPILER.getStandardFileManager(null, null, null);

    static {
        JavaParser.setCacheParser(false);
    }


    private @NotNull final FilteringDiagnosticCollector myDiagnosticCollector = new FilteringDiagnosticCollector();
    private @Nullable Context               mySharedContext;
    private @Nullable ProcessingEnvironment environment;
    private @NotNull  AggregateCheckerEx    processor;

    public CheckerFrameworkCompilerImpl(final @NotNull Project project,
                                        final @NotNull Collection<String> compileOptions,
                                        final @NotNull Collection<Class<? extends Processor>> classes) {
        //myProject = project;
        mySharedContext = new Context();
        mySharedContext.put(DiagnosticListener.class, myDiagnosticCollector);
        final JavaFileManager fileManager = new PsiJavaFileManager(FILE_MANAGER, project);
        mySharedContext.put(JavaFileManager.class, fileManager);
        JavacTool.processOptions(mySharedContext, fileManager, compileOptions);
        Symtab.instance(mySharedContext);
        environment = JavacProcessingEnvironment.instance(mySharedContext);
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
    }

    @SuppressWarnings("UnusedDeclaration")
    @Nullable
    public List<Diagnostic<? extends JavaFileObject>> getMessages(@NotNull PsiClass psiClass) {
        assert mySharedContext != null;

        final Context context = mySharedContext;
        final Todo todo = Todo.instance(context);
        final Symtab symtab = Symtab.instance(context);

        final Names names = Names.instance(context);
        final Name name = names.fromString(psiClass.getQualifiedName());

        clean(psiClass, context);

        final Symbol.ClassSymbol classSymbol;
        try {
            // create new symbol
            classSymbol = ClassReader.instance(context).enterClass(
                name,
                new PsiClassJavaFileObject(psiClass)
            );
            classSymbol.complete();
        } catch (ProcessCanceledException e) {
            clean(psiClass, context);
            return null;
        }

        final JavaCompiler compiler = JavaCompiler.instance(context);
        // attribute newly created symbol(s)
        while (!todo.isEmpty()) {
            final Env<AttrContext> current = todo.remove();
            try {
                compiler.flow(compiler.attribute(current));
                final Name currentName = current.enclClass.type.asElement().getQualifiedName();
                if (currentName == name) {
                    final TreePath path = Trees.instance(environment).getPath(classSymbol);
                    processor.typeProcess(classSymbol, path);
                    break;
                }
            } catch (ProcessCanceledException ignored) {
                clean(psiClass, context);
                return null;
            }
        }

        LOG.debug(myDiagnosticCollector.getDiagnostics().size() + " diagnostics collected");
        return myDiagnosticCollector.getAndClear();
    }

    public static void clean(PsiClass psiClass, Context context) {
        final Symtab symtab = Symtab.instance(context);
        final Check check = Check.instance(context);
        final Names names = Names.instance(context);
        final Name name = names.fromString(psiClass.getQualifiedName());
        symtab.classes.remove(name);
        check.compiled.remove(name);
        for (PsiClass inner : psiClass.getInnerClasses()) {
            check.compiled.remove(names.fromString(psiClass.getQualifiedName() + "$" + inner.getName()));
            clean(inner, context);
        }
    }

    private abstract static class AggregateCheckerEx extends AggregateChecker {

        @Override
        public void setProcessingEnvironment(ProcessingEnvironment env) {
            super.setProcessingEnvironment(env);
        }
    }
}
