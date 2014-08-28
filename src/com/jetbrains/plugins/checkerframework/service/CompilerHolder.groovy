package com.jetbrains.plugins.checkerframework.service

import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.ServiceManager
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiJavaFile
import com.jetbrains.plugins.checkerframework.util.CheckerFrameworkSharedCompiler
import groovy.transform.CompileStatic
import org.jetbrains.annotations.NotNull
import org.jetbrains.annotations.Nullable

import java.util.concurrent.Future

@CompileStatic
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
    public List<ProblemDescriptor> processFile(@NotNull PsiJavaFile psiJavaFile, boolean isOnTheFly) {
        if (implementation == null) {
            if (isOnTheFly) {
                implementation = resetFuture?.done ? resetFuture.get() : null
            } else {
                resetSync()
            }
        }
        return implementation?.processFile(psiJavaFile, isOnTheFly);
    }

    public static CompilerHolder getInstance(Project project) {
        return ServiceManager.getService(project, CompilerHolder.class);
    }
}
