package com.jetbrains.plugins.checkerframework.inspection;

import com.intellij.codeInspection.AbstractBaseJavaLocalInspectionTool;
import com.intellij.codeInspection.InspectionManager;
import com.intellij.codeInspection.ProblemDescriptor;
import com.intellij.codeInspection.ProblemHighlightType;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.jetbrains.plugins.checkerframework.configurable.CheckerFrameworkSettings;
import com.jetbrains.plugins.checkerframework.inspection.util.CompositeChecker;
import com.jetbrains.plugins.checkerframework.inspection.util.VirtualJavaFileObject;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jps.cmdline.ClasspathBootstrap;

import javax.annotation.processing.Processor;
import javax.tools.*;
import javax.tools.JavaCompiler.*;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Locale;

/**
 * @author Daniil Ovchinnikov
 * @since 7/17/14
 */
public class AwesomeInspection extends AbstractBaseJavaLocalInspectionTool {

    private static final Logger LOG = Logger.getInstance(AwesomeInspection.class);
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

    @Nullable
    @Override
    public ProblemDescriptor[] checkFile(@NotNull PsiFile file, @NotNull InspectionManager manager, boolean isOnTheFly) {
        final CheckerFrameworkSettings mySettings = CheckerFrameworkSettings.getInstance(file.getProject());
        final Collection<Class<? extends Processor>> enabledCheckers = mySettings.getEnabledCheckerClasses();
        if (enabledCheckers.isEmpty()) {
            return null;
        }
        final Collection<String> COMPILE_OPTIONS = Arrays.asList(
            "-proc:only",
            "-AprintErrorStack", "-AprintAllQualifiers",
            "-classpath",
            mySettings.getPathToCheckerJar()
            + File.pathSeparator
            + ClasspathBootstrap.getResourcePath(JAVA_COMPILER.getClass())
        );
        final DiagnosticCollector<JavaFileObject> diagnosticCollector = new DiagnosticCollector<JavaFileObject>();
        final CompilationTask task = JAVA_COMPILER.getTask(
            null,
            FILE_MANAGER,
            diagnosticCollector,
            COMPILE_OPTIONS,
            null,
            Arrays.asList(new VirtualJavaFileObject(file))
        );
        task.setProcessors(
            Arrays.asList(
                new CompositeChecker(
                    mySettings.getEnabledCheckerClasses()
                )
            )
        );
        task.call();

        final Collection<ProblemDescriptor> problems = new ArrayList<ProblemDescriptor>();
        for (Diagnostic<? extends JavaFileObject> diagnostic : diagnosticCollector.getDiagnostics()) {
            final PsiElement startElement = file.findElementAt((int)diagnostic.getStartPosition());
            final PsiElement endElement = file.findElementAt((int)diagnostic.getEndPosition() - 1);
            if (startElement != null && startElement.isValid() && startElement.isPhysical()
                && endElement != null && endElement.isValid() && endElement.isPhysical()
                && startElement.getTextRange().getStartOffset() < endElement.getTextRange().getEndOffset()) {
                problems.add(
                    manager.createProblemDescriptor(
                        startElement,
                        endElement,
                        diagnostic.getMessage(Locale.getDefault()),
                        ProblemHighlightType.ERROR,
                        isOnTheFly
                    )
                );
            }
        }
        return problems.toArray(new ProblemDescriptor[problems.size()]);
    }
}
