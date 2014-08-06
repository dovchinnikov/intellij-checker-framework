package com.jetbrains.plugins.checkerframework.inspection;

import com.intellij.codeInspection.BaseJavaBatchLocalInspectionTool;
import com.intellij.codeInspection.InspectionManager;
import com.intellij.codeInspection.ProblemDescriptor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiFile;
import com.jetbrains.plugins.checkerframework.service.CheckerFrameworkCompiler;
import com.jetbrains.plugins.checkerframework.service.CheckerFrameworkProblemDescriptorBuilder;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.tools.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class AwesomeInspection extends BaseJavaBatchLocalInspectionTool {

    @Nullable
    @Override
    public ProblemDescriptor[] checkFile(@NotNull final PsiFile file, @NotNull final InspectionManager manager, final boolean isOnTheFly) {
        final Project project = manager.getProject();
        final List<Diagnostic<? extends JavaFileObject>> messages = CheckerFrameworkCompiler
            .getInstance(project)
            .getMessages(file);
        final CheckerFrameworkProblemDescriptorBuilder descriptorBuilder = CheckerFrameworkProblemDescriptorBuilder
            .getInstance(project);
        final Collection<ProblemDescriptor> problems = new ArrayList<ProblemDescriptor>();
        for (final Diagnostic<? extends JavaFileObject> diagnostic : messages) {
            ProblemDescriptor problemDescriptor = descriptorBuilder.buildProblemDescriptor(file, diagnostic, isOnTheFly);
            if (problemDescriptor != null) {
                problems.add(problemDescriptor);
            }
        }
        return problems.toArray(new ProblemDescriptor[problems.size()]);
    }
}
