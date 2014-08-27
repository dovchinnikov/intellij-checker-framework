package com.jetbrains.plugins.checkerframework.service

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.ServiceManager
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiJavaFile
import com.jetbrains.plugins.checkerframework.util.CheckerFrameworkSharedCompiler
import org.jetbrains.annotations.NotNull
import org.jetbrains.annotations.Nullable

import javax.tools.Diagnostic
import javax.tools.JavaFileObject
import java.util.concurrent.Future

public class CompilerHolder {

    private Project                                myProject
    private CheckerFrameworkSharedCompiler         implementation
    private Future<CheckerFrameworkSharedCompiler> resetFuture

    public CompilerHolder(Project project) {
        myProject = project;
        implementation = null;
        resetAsync();
    }

    public void resetAsync() {
        final CheckerFrameworkSettings settings = CheckerFrameworkSettings.getInstance(myProject);
        resetFuture = ApplicationManager.application.executeOnPooledThread(new GroovyCallable<CheckerFrameworkSharedCompiler>() {
            @Override
            CheckerFrameworkSharedCompiler call() throws Exception {
                return new CheckerFrameworkSharedCompiler(
                    myProject,
                    settings.options,
                    settings.enabledCheckerClasses,
                );
            }
        })
    }

    public void resetSync() {
        final CheckerFrameworkSettings settings = CheckerFrameworkSettings.getInstance(myProject);
        implementation = resetFuture?.get() ?: new CheckerFrameworkSharedCompiler(
            myProject,
            settings.options,
            settings.enabledCheckerClasses,
        );
    }

    @Nullable
    public List<Diagnostic<? extends JavaFileObject>> getMessages(
        @NotNull PsiJavaFile psiJavaFile, boolean isOnTheFly) {
        if (!isOnTheFly && implementation == null) {
            resetSync();
        }
        return implementation?.getMessages(psiJavaFile, isOnTheFly);
    }

    public static CompilerHolder getInstance(Project project) {
        return ServiceManager.getService(project, CompilerHolder.class);
    }
}
