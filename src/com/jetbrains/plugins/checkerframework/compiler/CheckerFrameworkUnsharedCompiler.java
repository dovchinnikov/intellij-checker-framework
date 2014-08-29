package com.jetbrains.plugins.checkerframework.compiler;

import com.intellij.codeInspection.ProblemDescriptor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiJavaFile;
import com.jetbrains.plugins.checkerframework.service.CheckerFrameworkProblemDescriptorBuilder;
import com.jetbrains.plugins.checkerframework.service.Stuff;
import com.jetbrains.plugins.checkerframework.tools.*;
import com.sun.source.util.TreePath;
import com.sun.source.util.Trees;
import com.sun.tools.javac.api.JavacTool;
import com.sun.tools.javac.comp.Enter;
import com.sun.tools.javac.jvm.ClassReader;
import com.sun.tools.javac.main.JavaCompiler;
import com.sun.tools.javac.processing.JavacProcessingEnvironment;
import com.sun.tools.javac.util.Context;
import com.sun.tools.javac.util.Name;
import com.sun.tools.javac.util.Names;
import org.checkerframework.framework.source.SourceChecker;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.annotation.processing.ProcessingEnvironment;
import javax.tools.Diagnostic;
import javax.tools.DiagnosticListener;
import javax.tools.JavaFileManager;
import javax.tools.JavaFileObject;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static com.sun.tools.javac.code.Symbol.ClassSymbol;

public class CheckerFrameworkUnsharedCompiler implements CheckerFrameworkCompiler {

    @SuppressWarnings({"FieldCanBeLocal", "UnusedDeclaration"}) private final Project myProject;
    private final ReusableDiagnosticCollector myReusableDiagnosticCollector = new ReusableDiagnosticCollector();
    private final CheckerFrameworkProblemDescriptorBuilder descriptorBuilder;
    private       AggregateCheckerEx                       processor;

    private final Context               mySharedContext;
    private final ProcessingEnvironment environment;
    private final Names                 names;
    private final ClassReader           classReader;

    public CheckerFrameworkUnsharedCompiler(final @NotNull Project project,
                                            final @NotNull Collection<String> compileOptions,
                                            final @NotNull Collection<Class<? extends SourceChecker>> classes) {
        myProject = project;
        descriptorBuilder = CheckerFrameworkProblemDescriptorBuilder.getInstance(project);
        mySharedContext = new ThreadContext();
        { // init context with own file manager
            ThreadedTrees.preRegister(mySharedContext);
            mySharedContext.put(DiagnosticListener.class, myReusableDiagnosticCollector);
            final JavaFileManager fileManager = new PsiJavaFileManager(FILE_MANAGER, project);
            mySharedContext.put(JavaFileManager.class, fileManager);
            JavacTool.processOptions(mySharedContext, fileManager, compileOptions);
        }
        names = Names.instance(mySharedContext);
        environment = JavacProcessingEnvironment.instance(mySharedContext);
        classReader = ClassReader.instance(mySharedContext);
        processor = new AggregateCheckerEx() {
            @Override
            protected Collection<Class<? extends SourceChecker>> getSupportedCheckers() {
                return classes;
            }
        };
        processor.setProcessingEnvironment(environment);
        processor.initChecker();
    }

    @Nullable
    public synchronized List<ProblemDescriptor> processFile(@NotNull final PsiJavaFile psiJavaFile) {

        // enter
        final List<ClassSymbol> classSymbols = new ArrayList<ClassSymbol>();
        for (PsiClass psiClass : psiJavaFile.getClasses()) {
            final Name name = getFlatName(psiClass);
            classSymbols.add(classReader.enterClass(name));
        }

        // complete
        for (ClassSymbol classSymbol : classSymbols) {
            if (classSymbol.classfile == null) {
                classSymbol.classfile = new PsiJavaFileObject(psiJavaFile);
            }
            classSymbol.complete();
        }

        // attr & flow
        final Enter enter = Enter.instance(mySharedContext);
        final JavaCompiler compiler = JavaCompiler.instance(mySharedContext);
        for (ClassSymbol classSymbol : classSymbols) {
            compiler.flow(compiler.attribute(enter.getClassEnv(classSymbol)));
        }

        // prepare problems collector
        final List<ProblemDescriptor> problems = new ArrayList<ProblemDescriptor>();
        myReusableDiagnosticCollector.setInternal(new DiagnosticListener<JavaFileObject>() {
            @Override
            public void report(final Diagnostic<? extends JavaFileObject> diagnostic) {
                if (!Stuff.PROC_CODE.equals(diagnostic.getCode())) return;
                final ProblemDescriptor problemDescriptor = descriptorBuilder.buildProblemDescriptor(
                    psiJavaFile,
                    diagnostic,
                    false
                );
                if (problemDescriptor != null) {
                    problems.add(problemDescriptor);
                }
            }
        });

        // process
        final Trees trees = Trees.instance(environment);
        for (ClassSymbol classSymbol : classSymbols) {
            TreePath tp = trees.getPath(classSymbol);
            processor.typeProcess(classSymbol, tp);
        }

        return problems;
    }

    protected synchronized Name getFlatName(PsiClass psiClass) {
        return names.fromString(
            psiClass.getContainingClass() == null
                ? psiClass.getQualifiedName()
                : psiClass.getContainingClass().getQualifiedName() + "$" + psiClass.getName()
        );
    }
}
