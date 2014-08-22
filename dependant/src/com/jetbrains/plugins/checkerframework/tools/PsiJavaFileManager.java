package com.jetbrains.plugins.checkerframework.tools;

import com.intellij.ide.highlighter.JavaClassFileType;
import com.intellij.ide.highlighter.JavaFileType;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiDirectory;
import com.intellij.psi.PsiPackage;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.search.ProjectScope;
import org.jetbrains.annotations.NotNull;

import javax.tools.ForwardingJavaFileManager;
import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.StandardLocation;
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
                                         String packageName,
                                         Set<JavaFileObject.Kind> kinds,
                                         boolean recurse) throws IOException {
        if (location == StandardLocation.PLATFORM_CLASS_PATH) {
//            if (packageName.startsWith("org.jetbrains.annotations")) {
//                return Collections.emptyList();
//            } else {
            return super.list(location, packageName, kinds, recurse);
//            }
        } else if (location != StandardLocation.CLASS_PATH) {
            return Collections.emptyList();
        } else if (packageName.startsWith("org.checkerframework")) {
            return super.list(location, packageName, kinds, recurse);
        }
//        for (JavaFileObject javaFileObject : fromSuper) {
//            result.add(javaFileObject);
//        }
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
                    try {
                        result.add(new VirtualJavaFileObject(file));
                    } catch (IllegalArgumentException ignored) {
                    }
                }
            }
        }
        if (kinds.contains(JavaFileObject.Kind.SOURCE)) {
            for (PsiClass psiClass : psiPackage.getClasses(mySourceScope)) {
                if (mySourceJavaFilesScope.contains(psiClass.getContainingFile().getVirtualFile())) {
                    try {
                        result.add(new PsiClassJavaFileObject(psiClass));
                    } catch (IllegalArgumentException ignored) {
                    }
                }
            }
        }
        return result;
    }

    @Override
    public String inferBinaryName(Location location, JavaFileObject file) {
        if (file instanceof VirtualJavaFileObject || file instanceof PsiClassJavaFileObject) {
            return file.getName();
        } else {
            return super.inferBinaryName(location, file);
        }
    }
}
