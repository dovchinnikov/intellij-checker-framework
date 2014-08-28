package com.jetbrains.plugins.checkerframework.inspection;

import com.intellij.codeInspection.InspectionManager;
import com.intellij.codeInspection.LocalInspectionTool;
import com.intellij.codeInspection.ProblemDescriptor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiJavaFile;
import com.jetbrains.plugins.checkerframework.service.CheckerFrameworkSettings;
import com.jetbrains.plugins.checkerframework.service.CompilerHolder;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class AwesomeInspection extends LocalInspectionTool {

    @Nullable
    @Override
    public ProblemDescriptor[] checkFile(@NotNull PsiFile psiFile, @NotNull InspectionManager manager, boolean isOnTheFly) {
        assert psiFile instanceof PsiJavaFile;
        final PsiJavaFile javaFile = (PsiJavaFile) psiFile;

        final Project project = manager.getProject();
        final CheckerFrameworkSettings settings = CheckerFrameworkSettings.getInstance(project);
        if (settings.getEnabledCheckerClasses().isEmpty()) {
            return null;
        }

        final List<ProblemDescriptor> problems = CompilerHolder.getInstance(project).processFile(javaFile, isOnTheFly);
        if (problems == null || problems.isEmpty()) {
            return null;
        } else {
            return problems.toArray(new ProblemDescriptor[problems.size()]);
        }
    }
}
