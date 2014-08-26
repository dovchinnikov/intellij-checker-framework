package com.jetbrains.plugins.checkerframework.inspection;

import com.intellij.codeInspection.BaseJavaBatchLocalInspectionTool;
import com.intellij.codeInspection.InspectionManager;
import com.intellij.codeInspection.ProblemDescriptor;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiJavaFile;
import com.jetbrains.plugins.checkerframework.service.CheckerFrameworkProblemDescriptorBuilder;
import com.jetbrains.plugins.checkerframework.service.CheckerFrameworkSettings;
import com.jetbrains.plugins.checkerframework.service.CompilerHolder;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.tools.Diagnostic;
import javax.tools.JavaFileObject;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class AwesomeInspection extends BaseJavaBatchLocalInspectionTool {

    @SuppressWarnings("UnusedDeclaration")
    private static final Logger LOG = Logger.getInstance("#" + AwesomeInspection.class.getCanonicalName());

    @Nullable
    @Override
    public ProblemDescriptor[] checkFile(@NotNull PsiFile psiFile, @NotNull InspectionManager manager, boolean isOnTheFly) {
        assert psiFile instanceof PsiJavaFile;

        final PsiJavaFile javaFile = (PsiJavaFile) psiFile;
        final Project project = manager.getProject();
        final CheckerFrameworkSettings settings = CheckerFrameworkSettings.getInstance(project);
        if (settings.getEnabledCheckerClasses().isEmpty()) {
            // settings are not valid or there are no configured checkers
            return null;
        }

        final List<Diagnostic<? extends JavaFileObject>> messages = CompilerHolder.getInstance(project).getMessages(javaFile, isOnTheFly);
        if (messages == null || messages.isEmpty()) {
            return null;
        }

        final CheckerFrameworkProblemDescriptorBuilder descriptorBuilder
            = CheckerFrameworkProblemDescriptorBuilder.getInstance(project);
        final Collection<ProblemDescriptor> problems = new ArrayList<ProblemDescriptor>();
        for (final Diagnostic<? extends JavaFileObject> diagnostic : messages) {
            final ProblemDescriptor problemDescriptor = descriptorBuilder.buildProblemDescriptor(
                javaFile,
                diagnostic,
                isOnTheFly
            );
            if (problemDescriptor != null) {
                problems.add(problemDescriptor);
            }
        }
        return problems.toArray(new ProblemDescriptor[problems.size()]);
    }
}
