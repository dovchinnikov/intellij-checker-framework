package com.jetbrains.plugins.checkerframework.configurable;

import com.intellij.openapi.diagnostic.Logger;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

public class ClassScanner {

    private static final Logger LOG = Logger.getInstance(ClassScanner.class);

    /**
     * Gets jar file of given {@code superclass}, and scans it for children of {@code superclass}.
     *
     * @param superclass parent class.
     * @param <T>        type of parent class.
     * @return empty list if class is loaded not from jar or jar cannot be opened,
     * otherwise list of non-abstract {@link java.lang.Class} objects
     * which are children of given {@code superclass}.
     */
    public static
    @NotNull
    <T> List<Class<? extends T>> findChildren(final @NotNull Class<T> superclass) {
        final List<Class<? extends T>> result = new ArrayList<Class<? extends T>>();
        final String superclassURL = superclass.getResource("").toString();

        int index = superclassURL.indexOf('!');
        if (index == -1) {
            return result;
        }

        final String jarURL = superclassURL.substring(0, index).replace("jar:file:", "");
        JarFile jar = null;
        try {
            jar = new JarFile(jarURL);

            for (final JarEntry entry : Collections.list(jar.entries())) {
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
                    } catch (ClassNotFoundException e) {
                        LOG.error(e);
                    } catch (ClassCastException e) {
                        LOG.debug(e);
                    }
                }
            }
        } catch (IOException e) {
            LOG.error(e);
        } finally {
            try {
                if (jar != null) {
                    jar.close();
                }
            } catch (IOException e) {
                LOG.error(e);
            }
        }

        return result;
    }
}
