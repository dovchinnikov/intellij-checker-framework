package com.jetbrains.plugins.checkerframework.service;

import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiFile;
import com.jetbrains.plugins.checkerframework.util.VirtualJavaFileObject;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jps.cmdline.ClasspathBootstrap;

import javax.annotation.processing.Processor;
import javax.tools.*;
import javax.tools.JavaCompiler.*;
import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public class CheckerFrameworkCompiler {

    private static final Logger LOG = Logger.getInstance(CheckerFrameworkCompiler.class);
    private static final JavaCompiler JAVA_COMPILER;
    private static final String AGGREGATE_PROCESSOR_FQN = "org.checkerframework.framework.source.AggregateCheckerEx";

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

    @NotNull
    public List<Diagnostic<? extends JavaFileObject>> getMessages(@NotNull PsiFile file) {
        if (mySettings.getEnabledCheckerClasses().isEmpty()) {
            return Collections.emptyList();
        }
        final Processor processor = createAggregateChecker();
        if (processor == null) {
            return Collections.emptyList();
        }
        final DiagnosticCollector<JavaFileObject> collector = new DiagnosticCollector<JavaFileObject>();
        final CompilationTask task = JAVA_COMPILER.getTask(
            null,
            FILE_MANAGER,
            collector,
            createCompileOptions(),
            null,
            Arrays.asList(new VirtualJavaFileObject(file))
        );
        task.setProcessors(Arrays.asList(processor));
        task.call();
        return collector.getDiagnostics();
    }

    @NotNull
    private Collection<String> createCompileOptions() {
        return Arrays.asList(
            "-proc:only",
            "-AprintErrorStack", "-AprintAllQualifiers",
            "-classpath",
            mySettings.getPathToCheckerJar()
            + File.pathSeparator
            + ClasspathBootstrap.getResourcePath(JAVA_COMPILER.getClass())
        );
    }

    @Nullable
    private Processor createAggregateChecker() {
        try {
            final Class<?> clazz = mySettings.getClassLoader().loadClass(AGGREGATE_PROCESSOR_FQN);
            final Method m = clazz.getDeclaredMethod("create", Collection.class);
            return (Processor)m.invoke(null, mySettings.getEnabledCheckerClasses());
        } catch (ClassNotFoundException e) {
            LOG.error(e);
        } catch (NoSuchMethodException e) {
            LOG.error(e);
        } catch (InvocationTargetException e) {
            LOG.error(e);
        } catch (IllegalAccessException e) {
            LOG.error(e);
        }
        return null;
    }

    public static CheckerFrameworkCompiler getInstance(@NotNull Project project) {
        return ServiceManager.getService(project, CheckerFrameworkCompiler.class);
    }
}
