package com.jetbrains.plugins.checkerframework.inspection;

import com.intellij.codeInspection.BaseJavaBatchLocalInspectionTool;
import com.intellij.codeInspection.InspectionManager;
import com.intellij.codeInspection.ProblemDescriptor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiClass;
import com.jetbrains.plugins.checkerframework.service.CheckerFrameworkCompiler;
import com.jetbrains.plugins.checkerframework.service.CheckerFrameworkProblemDescriptorBuilder;
import com.jetbrains.plugins.checkerframework.service.CheckerFrameworkSettings;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.tools.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class AwesomeInspection extends BaseJavaBatchLocalInspectionTool {

    @Nullable
    @Override
    public ProblemDescriptor[] checkClass(@NotNull PsiClass clazz, @NotNull InspectionManager manager, boolean isOnTheFly) {
        //if (true) {
        //    return null;
        //}
        if (clazz.getContainingClass() != null) {
            return null;
        }
        final Project project = manager.getProject();
        final CheckerFrameworkSettings settings = CheckerFrameworkSettings.getInstance(project);
        if (!settings.isValid() || settings.getEnabledCheckers().isEmpty()) {
            return null;
        }
        final List<Diagnostic<? extends JavaFileObject>> messages = CheckerFrameworkCompiler
            .getInstance(project)
            .getMessages(clazz);
        final CheckerFrameworkProblemDescriptorBuilder descriptorBuilder = CheckerFrameworkProblemDescriptorBuilder
            .getInstance(project);
        final Collection<ProblemDescriptor> problems = new ArrayList<ProblemDescriptor>();
        for (final Diagnostic<? extends JavaFileObject> diagnostic : messages) {
            final ProblemDescriptor problemDescriptor
                = descriptorBuilder.buildProblemDescriptor(clazz.getContainingFile(), diagnostic, isOnTheFly);
            if (problemDescriptor != null) {
                problems.add(problemDescriptor);
            }
        }
        return problems.toArray(new ProblemDescriptor[problems.size()]);
    }
}
