package com.jetbrains.plugins.checkerframework.util;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.progress.ProcessCanceledException;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiClass;
import com.jetbrains.plugins.checkerframework.service.CheckerFrameworkCompiler;
import com.jetbrains.plugins.checkerframework.tools.FilteringDiagnosticCollector;
import com.jetbrains.plugins.checkerframework.tools.PsiClassJavaFileObject;
import com.jetbrains.plugins.checkerframework.tools.PsiJavaFileManager;
import com.sun.source.util.TaskEvent;
import com.sun.source.util.TaskListener;
import com.sun.source.util.TreePath;
import com.sun.source.util.Trees;
import com.sun.tools.javac.api.JavacTool;
import com.sun.tools.javac.api.MultiTaskListener;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.code.Symtab;
import com.sun.tools.javac.comp.*;
import com.sun.tools.javac.jvm.ClassReader;
import com.sun.tools.javac.main.JavaCompiler;
import com.sun.tools.javac.processing.JavacProcessingEnvironment;
import com.sun.tools.javac.util.Context;
import com.sun.tools.javac.util.Name;
import com.sun.tools.javac.util.Names;
import org.checkerframework.framework.source.AggregateChecker;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.Processor;
import javax.lang.model.element.TypeElement;
import javax.tools.*;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public class CheckerFrameworkCompilerImpl implements CheckerFrameworkCompiler {

    private static final Logger                  LOG           = Logger.getInstance(CheckerFrameworkCompilerImpl.class);
    private static final JavacTool               JAVA_COMPILER = JavacTool.create();
    private static final StandardJavaFileManager FILE_MANAGER  = JAVA_COMPILER.getStandardFileManager(null, null, null);

    private @NotNull final FilteringDiagnosticCollector myDiagnosticCollector = new FilteringDiagnosticCollector();
    //private final     Project                  myProject;
    private @Nullable Context               mySharedContext;
    private @Nullable ProcessingEnvironment environment;
    private @NotNull  AggregateChecker      processor;

    public CheckerFrameworkCompilerImpl(@NotNull Project project,
                                        Collection<String> compileOptions,
                                        Collection<Class<? extends Processor>> classes) {
        //myProject = project;
        mySharedContext = new Context();
        mySharedContext.put(DiagnosticListener.class, myDiagnosticCollector);
        final JavaFileManager fileManager = new PsiJavaFileManager(FILE_MANAGER, project);
        mySharedContext.put(JavaFileManager.class, fileManager);
        JavacTool.processOptions(mySharedContext, fileManager, compileOptions);
        Symtab.instance(mySharedContext);
        environment = JavacProcessingEnvironment.instance(mySharedContext);
        processor = AggregateCheckerEx.createAndInit(classes, environment);
    }

    @NotNull
    @Override

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
            return Collections.emptyList();
        }

        final MultiTaskListener multiTaskListener = MultiTaskListener.instance(context);
        final TaskListener listener = new TaskListener() {
            @Override
            public void started(TaskEvent e) {
            }

            @Override
            public void finished(TaskEvent e) {
                if (e.getKind() == TaskEvent.Kind.ANALYZE) {
                    final TypeElement typeElement = e.getTypeElement();
                    if (typeElement.getQualifiedName() != name) {
                        return;
                    }
                    final TreePath path = Trees.instance(environment).getPath(typeElement);
                    // invoke type processing
                    processor.typeProcess(typeElement, path);
                    throw new EnoughException();
                }
            }
        };
        multiTaskListener.add(listener);

        final JavaCompiler compiler = JavaCompiler.instance(context);
        // attribute newly created symbol(s)
        while (!todo.isEmpty()) {
            final Env<AttrContext> current = todo.remove();
            try {
                compiler.flow(compiler.attribute(current));
            } catch (ProcessCanceledException ignored) {
                CompileStates.instance(context).remove(current);
                symtab.classes.remove(current.enclClass.type.asElement().getQualifiedName());
                return Collections.emptyList();
            } catch (EnoughException ignored) {
                break;
            } finally {
                multiTaskListener.remove(listener);
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

    private static class EnoughException extends RuntimeException {
    }
}
