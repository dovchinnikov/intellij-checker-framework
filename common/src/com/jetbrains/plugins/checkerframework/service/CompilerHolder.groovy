package com.jetbrains.plugins.checkerframework.service

import com.intellij.openapi.components.ServiceManager
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiClass
import org.jetbrains.annotations.NotNull
import org.jetbrains.annotations.Nullable

import javax.tools.Diagnostic
import javax.tools.JavaFileObject

public class CompilerHolder {

    private @NotNull CheckerFrameworkSettings mySettings;
    private @Nullable def internal;

    public CompilerHolder(Project project) {
        mySettings = CheckerFrameworkSettings.getInstance(project);
    }

    @Nullable
    public List<Diagnostic<? extends JavaFileObject>> getMessages(@NotNull PsiClass psiClass) {
        if (internal == null && mySettings.valid()) {
            // try to create compiler
            internal = mySettings.createCompiler();
        }
        return internal?.getMessages(psiClass);
    }

    public void reset() {
        internal = null;
    }

    public static CompilerHolder getInstance(Project project) {
        return ServiceManager.getService(project, CompilerHolder.class);
    }
}
