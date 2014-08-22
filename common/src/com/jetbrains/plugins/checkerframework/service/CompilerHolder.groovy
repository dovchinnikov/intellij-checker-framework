package com.jetbrains.plugins.checkerframework.service

import com.intellij.openapi.components.ServiceManager
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiClass
import org.jetbrains.annotations.NotNull

import javax.tools.Diagnostic
import javax.tools.JavaFileObject

public class CompilerHolder implements CheckerFrameworkCompiler {

    private Project myProject
    private def     internal;

    public CompilerHolder(Project project) {
        myProject = project
    }

    @NotNull
    @Override
    public List<Diagnostic<? extends JavaFileObject>> getMessages(@NotNull PsiClass psiClass) {
//        return Collections.emptyList();
        if (internal == null) {
            internal = CheckerFrameworkSettings.getInstance(myProject).createCompiler();
        }
        return internal == null ? Collections.emptyList() : internal.getMessages(psiClass);
    }

    public void reset() {
        internal = null;
    }

    public static CompilerHolder getInstance(Project project) {
        return ServiceManager.getService(project, CompilerHolder.class);
    }
}
