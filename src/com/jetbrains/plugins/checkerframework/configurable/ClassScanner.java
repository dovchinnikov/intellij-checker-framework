package com.jetbrains.plugins.checkerframework.configurable;

import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 * @author Daniil Ovchinnikov.
 * @since 7/8/14.
 */
public class ClassScanner {

    public static
    @NotNull
    <T> List<Class<? extends T>> findChildren(final @NotNull Class<T> superclass) {
        final List<Class<? extends T>> result = new ArrayList<>();
        final String superclassURL = superclass.getResource("").toString();

        int index = superclassURL.indexOf('!');
        if (index == -1) {
            return result;
        }

        final String jarURL = superclassURL.substring(0, index).replace("jar:file:", "");
        final JarFile jar;
        try {
            jar = new JarFile(jarURL);
        } catch (IOException e) {
            e.printStackTrace();
            return result;
        }

        final Enumeration<JarEntry> entries = jar.entries();
        while (entries.hasMoreElements()) {
            final JarEntry entry = entries.nextElement();
            if (!entry.isDirectory() && entry.toString().endsWith(".class")) {
                try {
                    final Class<? extends T> clazz = Class.forName(
                            entry.toString()
                                    .replaceAll(File.separator, ".")
                                    .replace(".class", "")
                    ).asSubclass(superclass);
                    if (!Modifier.isAbstract(clazz.getModifiers()) && !clazz.isAnonymousClass()) {
                        result.add(clazz);
                    }
                } catch (ClassNotFoundException | ClassCastException ignored) {
                }
            }
        }

        return result;
    }
}
