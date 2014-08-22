package com.jetbrains.plugins.checkerframework.service;

import com.intellij.psi.PsiClass;
import org.jetbrains.annotations.NotNull;

import javax.tools.Diagnostic;
import javax.tools.JavaFileObject;
import java.util.List;

public interface CheckerFrameworkCompiler {

    @NotNull
    List<Diagnostic<? extends JavaFileObject>> getMessages(@NotNull PsiClass psiClass);

}
