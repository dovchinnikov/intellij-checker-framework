package com.jetbrains.plugins.checkerframework.util;

import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.project.Project;
import com.intellij.psi.*;
import com.intellij.psi.impl.file.PsiJavaDirectoryImpl;
import org.jetbrains.annotations.NotNull;

import java.util.HashSet;
import java.util.Set;

public class CheckerFrameworkModificationListener extends PsiTreeChangeAdapter {

    public Set<PsiJavaFile> getChangedFiles() {
        try {
            return new HashSet<PsiJavaFile>(myChangedFiles);
        } finally {
            myChangedFiles.clear();
        }
    }

    public Set<PsiJavaFile> getRemovedFiles() {
        return myRemovedFiles;
    }

    private final Set<PsiJavaFile> myChangedFiles = new HashSet<PsiJavaFile>();
    private final Set<PsiJavaFile> myRemovedFiles = new HashSet<PsiJavaFile>();

    public CheckerFrameworkModificationListener(Project project) {
        PsiManager.getInstance(project).addPsiTreeChangeListener(this);
    }

    private void cleanLater(PsiFile file) {
        if (file instanceof PsiJavaFile) {
            myChangedFiles.add((PsiJavaFile) file);
        }
    }

    private void cleanLater(PsiTreeChangeEvent event) {
        cleanLater(event.getFile());
    }

    @Override
    public void childAdded(@NotNull PsiTreeChangeEvent event) {
        cleanLater(event);
    }

    @Override
    public void childRemoved(@NotNull PsiTreeChangeEvent event) {
        if (event.getParent() instanceof PsiJavaDirectoryImpl) {
            cleanLater(event);
        }
    }

    @Override
    public void childReplaced(@NotNull PsiTreeChangeEvent event) {
        cleanLater(event);
    }

    @Override
    public void childMoved(@NotNull PsiTreeChangeEvent event) {
        cleanLater(event);
    }

    @Override
    public void childrenChanged(@NotNull PsiTreeChangeEvent event) {
        cleanLater(event);
    }

    public static CheckerFrameworkModificationListener getInstance(Project project) {
        return ServiceManager.getService(project, CheckerFrameworkModificationListener.class);
    }
}
