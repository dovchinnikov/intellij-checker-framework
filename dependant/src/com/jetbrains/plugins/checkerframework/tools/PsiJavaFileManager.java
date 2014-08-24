package com.jetbrains.plugins.checkerframework.tools;

import com.intellij.ide.highlighter.JavaClassFileType;
import com.intellij.ide.highlighter.JavaFileType;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Computable;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.*;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.search.ProjectScope;
import org.jetbrains.annotations.NotNull;

import javax.tools.ForwardingJavaFileManager;
import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.StandardLocation;
import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class PsiJavaFileManager extends ForwardingJavaFileManager<StandardJavaFileManager> {

    private final Project           myProject;
    private final GlobalSearchScope mySourceScope;
    private final GlobalSearchScope mySourceJavaFilesScope;
    private final GlobalSearchScope myLibraryClassFilesScope;
    private final GlobalSearchScope myLibraryDirectoriesScope;

    public PsiJavaFileManager(StandardJavaFileManager fileManager, Project project) {
        super(fileManager);
        this.myProject = project;
        mySourceScope = ProjectScope.getProjectScope(project);
        mySourceJavaFilesScope = GlobalSearchScope.getScopeRestrictedByFileTypes(mySourceScope, JavaFileType.INSTANCE);
        myLibraryDirectoriesScope = ProjectScope.getLibrariesScope(project);
        myLibraryClassFilesScope = GlobalSearchScope.getScopeRestrictedByFileTypes(myLibraryDirectoriesScope, JavaClassFileType.INSTANCE);
    }

    @Override
    @NotNull
    public Iterable<JavaFileObject> list(Location location,
                                         final String packageName,
                                         final Set<JavaFileObject.Kind> kinds,
                                         boolean recurse) throws IOException {
        if (location == StandardLocation.PLATFORM_CLASS_PATH) {
            return super.list(location, packageName, kinds, recurse);
        } else if (location != StandardLocation.CLASS_PATH) {
            return Collections.emptyList();
        } else if (packageName.startsWith("org.checkerframework")) {
            return super.list(location, packageName, kinds, recurse);
        }

        return ApplicationManager.getApplication().runReadAction(new Computable<Iterable<JavaFileObject>>() {
            @Override
            public Iterable<JavaFileObject> compute() {
                final PsiPackage psiPackage = JavaPsiFacade.getInstance(myProject).findPackage(packageName);
                if (psiPackage == null) {
                    return Collections.emptyList();
                }
                final Set<JavaFileObject> result = new HashSet<JavaFileObject>();
                if (kinds.contains(JavaFileObject.Kind.CLASS)) {
                    final PsiDirectory[] psiDirectories = psiPackage.getDirectories(myLibraryDirectoriesScope);
                    for (PsiDirectory directory : psiDirectories) {
                        for (VirtualFile file : directory.getVirtualFile().getChildren()) {
                            if (file.isDirectory() || !myLibraryClassFilesScope.contains(file)) continue;
                            result.add(new VirtualJavaFileObject(file));
                        }
                    }
                }
                if (kinds.contains(JavaFileObject.Kind.SOURCE)) {
                    for (PsiClass psiClass : psiPackage.getClasses(mySourceScope)) {
                        if (mySourceJavaFilesScope.contains(psiClass.getContainingFile().getVirtualFile())) {
                            result.add(new PsiJavaFileObject((PsiJavaFile) psiClass.getContainingFile()));
                        }
                    }
                }
                return result;
            }
        });
    }

    @Override
    public String inferBinaryName(Location location, final JavaFileObject file) {
        if (file instanceof PsiJavaFileObject) {
            return ApplicationManager.getApplication().runReadAction(new Computable<String>() {
                @Override
                public String compute() {
                    final PsiJavaFile javaFile = ((PsiJavaFileObject) file).myJavaFile;
                    final String packageName = javaFile.getPackageName();
                    final String className = javaFile.getName().replace(".java", "");
                    return packageName.isEmpty() ? className : packageName + "." + className;
                }
            });
        } else if (file instanceof VirtualJavaFileObject) {
            final VirtualFile virtualFile = ((VirtualJavaFileObject) file).myFile;
            String rawPath = virtualFile.getPath();
            int index = rawPath.indexOf('!');
            if (index > -1) {
                rawPath = rawPath.substring(index + 1);
            }
            if (rawPath.startsWith(File.separator)) {
                rawPath = rawPath.substring(File.separator.length());
            }
            if (rawPath.endsWith(".class")) {
                rawPath = rawPath.replace(".class", "");
            }
            return rawPath.replace(File.separatorChar, '.');
        } else {
            return super.inferBinaryName(location, file);
        }
    }
}
