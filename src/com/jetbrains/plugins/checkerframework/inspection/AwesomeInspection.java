package com.jetbrains.plugins.checkerframework.inspection;

import com.intellij.codeInspection.BaseJavaBatchLocalInspectionTool;
import com.intellij.codeInspection.InspectionManager;
import com.intellij.codeInspection.ProblemDescriptor;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiClass;
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
    public ProblemDescriptor[] checkClass(@NotNull PsiClass clazz, @NotNull InspectionManager manager, boolean isOnTheFly) {
        if (clazz.getQualifiedName() == null || clazz.getContainingClass() != null) {
            // null when inner or anonymous class
            return null;
        }

        final Project project = manager.getProject();

        final CheckerFrameworkSettings settings = CheckerFrameworkSettings.getInstance(project);
        if (!settings.valid() || settings.getEnabledCheckers().isEmpty()) {
            // settings are not valid or there are no configured checkers
            return null;
        }

        final List<Diagnostic<? extends JavaFileObject>> messages = CompilerHolder.getInstance(project).getMessages(clazz);
        if (messages == null || messages.isEmpty()) {
            return null;
        }

        final CheckerFrameworkProblemDescriptorBuilder descriptorBuilder
            = CheckerFrameworkProblemDescriptorBuilder.getInstance(project);
        final Collection<ProblemDescriptor> problems = new ArrayList<ProblemDescriptor>();
        for (final Diagnostic<? extends JavaFileObject> diagnostic : messages) {
            final ProblemDescriptor problemDescriptor = descriptorBuilder.buildProblemDescriptor(
                clazz.getContainingFile(),
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
