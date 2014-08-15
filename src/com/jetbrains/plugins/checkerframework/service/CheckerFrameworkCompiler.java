package com.jetbrains.plugins.checkerframework.service;

import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiClass;
import com.jetbrains.plugins.checkerframework.util.FilteringDiagnosticCollector;
import com.jetbrains.plugins.checkerframework.util.PsiJavaFileObject;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jps.cmdline.ClasspathBootstrap;

import javax.annotation.processing.Processor;
import javax.tools.*;
import java.io.File;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import static javax.tools.JavaCompiler.*;

public class CheckerFrameworkCompiler {

    private static final Logger LOG = Logger.getInstance(CheckerFrameworkCompiler.class);
    private static final JavaCompiler JAVA_COMPILER;

    static {
        try {
            JAVA_COMPILER = (JavaCompiler)Class.forName(
                ToolProvider.getSystemJavaCompiler().getClass().getCanonicalName()
            ).newInstance();
        } catch (InstantiationException e) {
            LOG.error(e);
            throw new RuntimeException(e);
        } catch (IllegalAccessException e) {
            LOG.error(e);
            throw new RuntimeException(e);
        } catch (ClassNotFoundException e) {
            LOG.error(e);
            throw new RuntimeException(e);
        }
    }

    private static final StandardJavaFileManager FILE_MANAGER = JAVA_COMPILER.getStandardFileManager(null, null, null);


    private final @NotNull Project                  myProject;
    private final @NotNull CheckerFrameworkSettings mySettings;
    //private final          Context                       mySharedContext;
    //private final          PsiClassReader           myReader;
    private final          PsiJavaFileManager       myFileManager;
    //private final          Names                    myNames;
    //private final          Symtab                   mySymtab;

    private final FilteringDiagnosticCollector myDiagnosticCollector = new FilteringDiagnosticCollector();
    //private final Map<Name, Symbol.ClassSymbol>   classCache            = new HashMap<>();
    //private final Map<Name, Symbol.PackageSymbol> packageCache          = new HashMap<>();

    public CheckerFrameworkCompiler(@NotNull Project project) {
        myProject = project;
        mySettings = CheckerFrameworkSettings.getInstance(project);
        myFileManager = new PsiJavaFileManager(FILE_MANAGER, project);
        //mySharedContext = new Context();
        //mySharedContext.put(
        //    PsiClassReader.getKey(), (Context.Factory<ClassReader>)c -> new PsiClassReader(mySharedContext, myProject)
        //);
        //mySharedContext.put(JavaFileManager.class, FILE_MANAGER);
        //Context context = new Context();
        //Context ctx = new Context();
        //ctx.put(JavaFileManager.class, myFileManager);
        //myNames = Names.instance(ctx);
        //mySymtab = Symtab.instance(ctx);
        //myReader = new PsiClassReader(new Context(), myProject, classCache, packageCache);
    }

    public static CheckerFrameworkCompiler getInstance(@NotNull Project project) {
        return ServiceManager.getService(project, CheckerFrameworkCompiler.class);
    }

    @NotNull
    public List<Diagnostic<? extends JavaFileObject>> getMessages(@NotNull PsiClass file) {
        if (mySettings.getEnabledCheckers().isEmpty()) {
            return Collections.emptyList();
        }
        final Processor processor = mySettings.createAggregateChecker();
        if (processor == null) {
            return Collections.emptyList();
        }
        //final Context context = new Context();
        //context.put(JavaFileManager.class, myFileManager);
        //context.put(DiagnosticListener.class, myDiagnosticCollector);
        //context.put(Names.namesKey, myNames);
        //context.put(PsiSymtab.getKey(), mySymtab);
        //final Processor processor = new FenumChecker();
        //final Iterable<? extends JavaFileObject> compilationUnits = Arrays.asList(
        //    new PsiJavaFileObject(file)
        //);
        final Iterable<String> options = createCompileOptions();
        final CompilationTask task =
            //PsiJavacTool.getTask(options, context, compilationUnits);
            JAVA_COMPILER.getTask(
                null,
                myFileManager,
                myDiagnosticCollector,
                options,
                null,
                Arrays.asList(new PsiJavaFileObject(file))
            );

        task.setProcessors(Arrays.asList(processor));
        task.call();
        return myDiagnosticCollector.getAndClear();
    }

    @NotNull
    private Collection<String> createCompileOptions() {
        return Arrays.asList(
            "-proc:only",
            "-AprintErrorStack",
            "-Adetailedmsgtext",
            "-classpath",
            mySettings.getPathToCheckerJar()
            + File.pathSeparator
            + ClasspathBootstrap.getResourcePath(JAVA_COMPILER.getClass())
        );
    }
}
