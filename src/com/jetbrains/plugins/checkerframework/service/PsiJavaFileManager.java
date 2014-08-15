package com.jetbrains.plugins.checkerframework.service;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiDirectory;
import com.intellij.psi.PsiPackage;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.search.ProjectScope;
import com.jetbrains.plugins.checkerframework.util.PsiJavaFileObject;

import javax.tools.*;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class PsiJavaFileManager extends ForwardingJavaFileManager<StandardJavaFileManager> {

    private final Project           myProject;
    private final GlobalSearchScope mySourceScope;
    private final GlobalSearchScope myLibrariesScope;

    /**
     * Creates a new instance of ForwardingJavaFileManager.
     *
     * @param fileManager delegate to this file manager
     * @param project
     */
    public PsiJavaFileManager(StandardJavaFileManager fileManager, Project project) {
        super(fileManager);
        this.myProject = project;
        mySourceScope = ProjectScope.getProjectScope(project);
        myLibrariesScope = ProjectScope.getLibrariesScope(project);
    }

    @Override
    public Iterable<JavaFileObject> list(Location location,
                                         String packageName,
                                         Set<JavaFileObject.Kind> kinds,
                                         boolean recurse) throws IOException {
        final Iterable<JavaFileObject> fromSuper = super.list(location, packageName, kinds, recurse);
        if (location == StandardLocation.PLATFORM_CLASS_PATH) {
            return fromSuper;
        }
        final List<JavaFileObject> result = new ArrayList<JavaFileObject>();
        //if (result.isEmpty()) {
        final PsiPackage psiPackage = JavaPsiFacade.getInstance(myProject).findPackage(packageName);
        if (psiPackage != null) {
            if (kinds.contains(JavaFileObject.Kind.SOURCE)) {
                for (PsiClass psiClass : psiPackage.getClasses(mySourceScope)) {
                    result.add(new PsiJavaFileObject(psiClass));
                }
            }
            if (kinds.contains(JavaFileObject.Kind.CLASS)) {
                final PsiDirectory[] psiDirectories = psiPackage.getDirectories(myLibrariesScope);
                for (PsiDirectory directory : psiDirectories) {
                    for (VirtualFile file : directory.getVirtualFile().getChildren()) {
                        if (file.isDirectory()) continue;
                        result.add(new PsiJavaFileObject(file));
                    }
                }
            }
        }
        //}
        return result;
    }

    @Override
    public String inferBinaryName(Location location, JavaFileObject file) {
        if (file instanceof PsiJavaFileObject) {
            return ((PsiJavaFileObject)file).name;
        } else {
            return super.inferBinaryName(location, file);
        }
    }
}
