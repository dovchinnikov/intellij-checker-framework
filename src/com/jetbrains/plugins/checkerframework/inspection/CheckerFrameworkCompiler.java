package com.jetbrains.plugins.checkerframework.inspection;

import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiFile;
import com.jetbrains.plugins.checkerframework.configurable.CheckerFrameworkSettings;
import com.jetbrains.plugins.checkerframework.inspection.util.CompositeChecker;
import com.jetbrains.plugins.checkerframework.inspection.util.VirtualJavaFileObject;
import com.sun.source.util.JavacTask;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jps.cmdline.ClasspathBootstrap;

import javax.tools.*;
import javax.tools.JavaCompiler.*;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

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

    private final @NotNull CheckerFrameworkSettings mySettings;

    public CheckerFrameworkCompiler(@NotNull Project project) {
        mySettings = CheckerFrameworkSettings.getInstance(project);
    }

    public Collection<String> createCompileOptions() {
        return Arrays.asList(
            "-proc:only",
            "-AprintErrorStack", "-AprintAllQualifiers",
            "-classpath",
            mySettings.getPathToCheckerJar()
            + File.pathSeparator
            + ClasspathBootstrap.getResourcePath(JAVA_COMPILER.getClass())
        );
    }

    @NotNull
    public CompilationTask createCompilationTask(@NotNull DiagnosticListener<? super JavaFileObject> diagnosticListener,
                                                 @NotNull PsiFile file) {
        final CompilationTask task = JAVA_COMPILER.getTask(
            null,
            FILE_MANAGER,
            diagnosticListener,
            createCompileOptions(),
            null,
            Arrays.asList(new VirtualJavaFileObject(file))
        );
        task.setProcessors(
            Arrays.asList(
                new CompositeChecker(mySettings.getEnabledCheckerClasses(), (JavacTask)task)
            )
        );
        return task;
    }

    public static CheckerFrameworkCompiler getInstance(@NotNull Project project) {
        return ServiceManager.getService(project, CheckerFrameworkCompiler.class);
    }

    @NotNull
    public List<Diagnostic<? extends JavaFileObject>> getMessages(@NotNull PsiFile file) {
        if (mySettings.getEnabledCheckerClasses().isEmpty()) {
            return new ArrayList<Diagnostic<? extends JavaFileObject>>();
        }
        final DiagnosticCollector<JavaFileObject> collector = new DiagnosticCollector<JavaFileObject>();
        createCompilationTask(collector, file).call();
        return collector.getDiagnostics();
    }
}
