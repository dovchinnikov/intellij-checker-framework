package com.jetbrains.plugins.checkerframework.service

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.ServiceManager
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiJavaFile
import com.jetbrains.plugins.checkerframework.util.CheckerFrameworkCompilerImpl
import org.checkerframework.framework.source.SourceChecker
import org.jetbrains.annotations.NotNull
import org.jetbrains.annotations.Nullable

import javax.tools.Diagnostic
import javax.tools.JavaFileObject

public class CompilerHolder {

    private Project                      myProject
    private CheckerFrameworkCompilerImpl implementation

    public CompilerHolder(Project project) {
        myProject = project;
        CheckerFrameworkSettings settings = CheckerFrameworkSettings.getInstance(myProject);
        reset(settings.options, settings.enabledCheckerClasses);
    }

    public void reset(def ... stuff) {
        List<String> compileOptions;
        List<Class<? extends SourceChecker>> classes;
        (compileOptions, classes) = stuff;
        implementation = null;
        ApplicationManager.application.executeOnPooledThread({
            implementation = new CheckerFrameworkCompilerImpl(
                myProject,
                compileOptions,
                classes
            )
        })
    }

    @Nullable
    public List<Diagnostic<? extends JavaFileObject>> getMessages(@NotNull PsiJavaFile psiJavaFile) {
        return implementation?.getMessages(psiJavaFile);
    }

    public static CompilerHolder getInstance(Project project) {
        return ServiceManager.getService(project, CompilerHolder.class);
    }
}
