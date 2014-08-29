package com.jetbrains.plugins.checkerframework.service;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.roots.OrderRootType;
import com.intellij.openapi.roots.impl.libraries.ApplicationLibraryTable;
import com.intellij.openapi.roots.libraries.Library;
import com.intellij.openapi.util.Computable;
import org.jetbrains.annotations.NotNull;

import java.io.File;

public class CheckerFrameworkLibrary {

    public static boolean exists() {
        return ApplicationLibraryTable.getApplicationTable().getLibraryByName(Stuff.LIBRARY_NAME) != null;
    }

    public static @NotNull Library getOrCreateLibrary() {
        final ApplicationLibraryTable applicationLibraryTable = ApplicationLibraryTable.getApplicationTable();
        Library library = applicationLibraryTable.getLibraryByName(Stuff.LIBRARY_NAME);
        if (library == null) {
            library = ApplicationManager.getApplication().runWriteAction(new Computable<Library>() {
                @Override public Library compute() {
                    final Library library = applicationLibraryTable.createLibrary(Stuff.LIBRARY_NAME);
                    final Library.ModifiableModel modifiableModel = library.getModifiableModel();
                    modifiableModel.addJarDirectory(new File(Stuff.PATH_TO_CHECKER).getParent(), false, OrderRootType.CLASSES);
                    modifiableModel.commit();
                    return library;
                }
            });
        }
        return library;
    }
}
