package com.jetbrains.plugins.checkerframework.service

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.ServiceManager
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiClass
import org.jetbrains.annotations.NotNull
import org.jetbrains.annotations.Nullable

import javax.tools.Diagnostic
import javax.tools.JavaFileObject
import java.lang.reflect.Constructor
import java.util.concurrent.Future

public class CompilerHolder {

    private Project     myProject
    private Constructor constructor
    private Future      implementationInstanceFuture
    private def         implementation
    private def         compileOptions, classes

    public CompilerHolder(Project project) {
        myProject = project
    }

    public void reset(def ... stuff) {
        (constructor, compileOptions, classes) = stuff
        implementation = null
        implementationInstanceFuture = ApplicationManager.application.executeOnPooledThread({
            implementation = constructor.newInstance(
                myProject,
                compileOptions,
                classes
            )
        })
    }

    @Nullable
    public List<Diagnostic<? extends JavaFileObject>> getMessages(@NotNull PsiClass psiClass) {
        return implementation?.getMessages(psiClass);
    }

    public static CompilerHolder getInstance(Project project) {
        return ServiceManager.getService(project, CompilerHolder.class);
    }
}
