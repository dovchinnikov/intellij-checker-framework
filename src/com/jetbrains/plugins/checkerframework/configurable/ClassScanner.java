package com.jetbrains.plugins.checkerframework.configurable;

import com.intellij.openapi.diagnostic.Logger;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;

import javax.annotation.processing.Processor;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Modifier;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

public class ClassScanner {

    private static final Logger LOG = Logger.getInstance(ClassScanner.class);

    /**
     * Gets jar file of given {@code superclass}, and scans it for children of {@code superclass}.
     *
     * @param jarFile            jar file to scan.
     * @param superclassFQN      parent class fully qualified name.
     * @param packageRestriction package where classes should belong to.
     * @return empty list if jar cannot be opened or superclass not found,
     * otherwise list of non-abstract {@link java.lang.Class} objects
     * which are children of given {@code superclass}.
     */
    @NotNull
    public static Set<Class<? extends Processor>> findChildren(final @NotNull File jarFile,
                                                               final @NotNull String superclassFQN,
                                                               final @NotNull String packageRestriction,
                                                               final @NotNull ClassLoader parent) {
        final Set<Class<? extends Processor>> result = new HashSet<Class<? extends Processor>>();

        JarFile jar = null;
        try {
            //noinspection IOResourceOpenedButNotSafelyClosed
            jar = new JarFile(jarFile);
            final URLClassLoader jarClassLoader = new URLClassLoader(new URL[]{jarFile.toURI().toURL()}, parent);
            final Class<? extends Processor> superclazz = jarClassLoader.loadClass(superclassFQN).asSubclass(Processor.class);

            for (final JarEntry entry : Collections.list(jar.entries())) {
                if (!entry.isDirectory() && entry.toString().endsWith(".class")) {
                    try {
                        @NonNls final String clazzName = entry.toString()
                            .replaceAll(File.separator, ".")
                            .replace(".class", "");

                        final String caseIgnoredClazzName = clazzName.toLowerCase();
                        if (!caseIgnoredClazzName.startsWith(packageRestriction)
                            || caseIgnoredClazzName.contains("sub")
                            || caseIgnoredClazzName.contains("abstract")
                            || caseIgnoredClazzName.contains("adapter")) {
                            continue;
                        }

                        final Class<? extends Processor> clazz = jarClassLoader.loadClass(clazzName).asSubclass(superclazz);

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
            LOG.debug(e);
        } catch (ClassNotFoundException e) {
            LOG.debug(e);
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
