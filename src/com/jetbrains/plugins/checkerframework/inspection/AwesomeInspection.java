package com.jetbrains.plugins.checkerframework.inspection;

import com.intellij.codeInspection.AbstractBaseJavaLocalInspectionTool;
import com.intellij.codeInspection.InspectionManager;
import com.intellij.codeInspection.ProblemDescriptor;
import com.intellij.codeInspection.ProblemHighlightType;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import org.checkerframework.checker.regex.RegexChecker;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.tools.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import static javax.tools.JavaCompiler.*;

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
        final Iterable<? extends JavaFileObject> sources = FILE_MANAGER.getJavaFileObjects(
            file.getVirtualFile().getCanonicalPath()
        );
        final DiagnosticCollector<JavaFileObject> diagnosticCollector = new DiagnosticCollector<JavaFileObject>();
        final CompilationTask task = JAVA_COMPILER.getTask(
            null, FILE_MANAGER, diagnosticCollector,
            Arrays.asList(
                "-proc:only",
                "-classpath", "/opt/checker-framework-1.8.3/checker/dist/checker-qual.jar"
            ), null, sources
        );
        task.setProcessors(Arrays.asList(new RegexChecker()));
        task.call();

        final List<ProblemDescriptor> problems = new ArrayList<ProblemDescriptor>();
        for (Diagnostic<? extends JavaFileObject> diagnostic : diagnosticCollector.getDiagnostics()) {
            final PsiElement startElement = file.findElementAt((int)diagnostic.getStartPosition());
            final PsiElement endElement = file.findElementAt((int)diagnostic.getEndPosition() - 1);
            if (startElement != null && endElement != null) {
                problems.add(
                    manager.createProblemDescriptor(
                        startElement,
                        endElement,
                        diagnostic.getMessage(Locale.getDefault()),
                        ProblemHighlightType.GENERIC_ERROR,
                        isOnTheFly
                    )
                );
            } else {
                LOG.warn("start or end element not found");
            }
        }
        return problems.toArray(new ProblemDescriptor[problems.size()]);
    }
}
