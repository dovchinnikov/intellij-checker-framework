package com.jetbrains.plugins.checkerframework.inspection;

import com.intellij.codeInspection.*;
import com.intellij.codeInspection.ex.GlobalInspectionContextUtil;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiJavaFile;
import com.jetbrains.plugins.checkerframework.compiler.CheckerFrameworkCompiler;
import com.jetbrains.plugins.checkerframework.compiler.CheckerFrameworkUnsharedCompiler;
import com.jetbrains.plugins.checkerframework.service.CheckerFrameworkSettings;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class CheckerFrameworkGlobalInspection extends GlobalSimpleInspectionTool {

    private CheckerFrameworkCompiler checkerFrameworkCompiler;

    @Override
    public void initialize(@NotNull GlobalInspectionContext context) {
        super.initialize(context);
        Project project = context.getProject();
        CheckerFrameworkSettings settings = CheckerFrameworkSettings.getInstance(project);
        checkerFrameworkCompiler = new CheckerFrameworkUnsharedCompiler(project, settings.getOptions(), settings.getEnabledCheckerClasses());
    }

    @Override
    public void checkFile(@NotNull PsiFile file, @NotNull InspectionManager manager, @NotNull ProblemsHolder problemsHolder, @NotNull GlobalInspectionContext globalContext, @NotNull ProblemDescriptionsProcessor problemDescriptionsProcessor) {
        if (!(file instanceof PsiJavaFile)) return;

        final List<ProblemDescriptor> problemDescriptors = checkerFrameworkCompiler.processFile((PsiJavaFile) file);
        if (problemDescriptors != null) {
//            final RefEntity refEntity = ;
            for (ProblemDescriptor problemDescriptor : problemDescriptors) {
                problemDescriptionsProcessor.addProblemElement(
                    GlobalInspectionContextUtil.retrieveRefElement(problemDescriptor.getPsiElement(), globalContext),
                    problemDescriptor
                );
//                    problemsHolder.registerProblem(problemDescriptor);
            }
        }
    }
}

