package com.jetbrains.plugins.checkerframework.service;

import com.intellij.ide.plugins.cl.PluginClassLoader;
import com.intellij.openapi.components.*;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Condition;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.lang.UrlClassLoader;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.annotation.processing.Processor;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.URL;
import java.util.*;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

@State(
    name = "CheckerFrameworkPluginSettings",
    storages = {
        @Storage(
            id = "default",
            file = StoragePathMacros.PROJECT_CONFIG_DIR + "/checkerframework-plugin-settings.xml"
        )
    }
)
public class CheckerFrameworkSettings implements PersistentStateComponent<CheckerFrameworkSettings.State> {

    public static final String CHECKERS_BASE_CLASS_FQN = "org.checkerframework.framework.source.SourceChecker";
    public static final String CHECKERS_PACKAGE = "org.checkerframework.checker";
    public static final String AGGREGATE_PROCESSOR_FQN = "com.jetbrains.plugins.checkerframework.util.AggregateCheckerEx";
    private static final Logger LOG = Logger.getInstance(CheckerFrameworkSettings.class);
    private final @NotNull List<String> myCheckers = new ArrayList<String>();
    private final @NotNull List<Class<? extends Processor>> myCheckerClasses = new ArrayList<Class<? extends Processor>>();
    private @NotNull State myState = new State();
    private boolean needReload = true;
    private Method myAggregateProcessorFactoryMethod;

    @SuppressWarnings("UnusedDeclaration")
    public CheckerFrameworkSettings() {
    }

    public CheckerFrameworkSettings(@NotNull CheckerFrameworkSettings original) {
        this.loadState(original.getState());
    }

    public static CheckerFrameworkSettings getInstance(final Project project) {
        return ServiceManager.getService(project, CheckerFrameworkSettings.class);
    }

    @NotNull
    public String getPathToCheckerJar() {
        return myState.myPathToCheckerJar;
    }

    public void setPathToCheckerJar(@NotNull String pathToCheckerJar) {
        needReload = !myState.myPathToCheckerJar.equals(pathToCheckerJar);
        myState.myPathToCheckerJar = pathToCheckerJar;
    }

    @NotNull
    public Set<String> getBuiltInCheckers() {
        if (needReload) loadClasses();
        return myState.myBuiltInCheckers;
    }

    @NotNull
    public Set<String> getEnabledCheckers() {
        return myState.myEnabledCheckers;
    }

    @NotNull
    public List<String> getCheckers() {
        if (needReload) loadClasses();
        return myCheckers;
    }

    @NotNull
    public Set<Class<? extends Processor>> getEnabledCheckerClasses() {
        if (needReload) loadClasses();
        final Set<Class<? extends Processor>> result = new HashSet<Class<? extends Processor>>(myCheckerClasses);
        ContainerUtil.retainAll(result, new Condition<Class<? extends Processor>>() {
            @Override
            public boolean value(Class<? extends Processor> aClass) {
                return myState.myEnabledCheckers.contains(aClass.getCanonicalName());
            }
        });
        return result;
    }

    public void addCustomChecker(@NotNull String clazzFQN) {
        myState.myCustomCheckers.add(clazzFQN);
        refreshCheckers();
    }

    @NotNull
    public List<String> getOptions() {
        return myState.myOptions;
    }

    @NotNull
    @Override
    public State getState() {
        return myState;
    }

    @Override
    public void loadState(final State state) {
        needReload = needReload || !state.myPathToCheckerJar.equals(state.myPathToCheckerJar);
        myState = new State(state);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        CheckerFrameworkSettings settings = (CheckerFrameworkSettings)o;
        return myState.equals(settings.myState);
    }

    @Override
    public int hashCode() {
        return myState.hashCode();
    }

    @Nullable
    public Processor createAggregateChecker() {
        if (needReload) loadClasses();
        try {
            return (Processor)myAggregateProcessorFactoryMethod.invoke(null, getEnabledCheckerClasses());
        } catch (InvocationTargetException e) {
            LOG.error(e);
        } catch (IllegalAccessException e) {
            LOG.error(e);
        }
        return null;
    }

    private void loadClasses() {
        myCheckerClasses.clear();
        myState.myBuiltInCheckers.clear();
        final PluginClassLoader currentPluginClassLoader = (PluginClassLoader)this.getClass().getClassLoader();
        JarFile jar = null;
        try {
            final URL jarUrl = new File(myState.myPathToCheckerJar).toURI().toURL();
            final UrlClassLoader jarClassLoader = new PluginClassLoader(new ArrayList<URL>(currentPluginClassLoader.getUrls()) {{
                add(jarUrl);
            }}, new ClassLoader[]{currentPluginClassLoader}, currentPluginClassLoader.getPluginId(), "lol", null);
            final Class<? extends Processor> superclazz =
                jarClassLoader.loadClass(CHECKERS_BASE_CLASS_FQN).asSubclass(Processor.class);
            final Class<?> aggregateProcessorClass =
                jarClassLoader.loadClass(AGGREGATE_PROCESSOR_FQN);
            myAggregateProcessorFactoryMethod = aggregateProcessorClass.getDeclaredMethod("create", Collection.class);

            //noinspection IOResourceOpenedButNotSafelyClosed
            jar = new JarFile(jarUrl.getFile());
            for (final JarEntry entry : Collections.list(jar.entries())) {
                if (!entry.isDirectory() && entry.toString().endsWith(".class")) {
                    try {
                        @NonNls final String clazzName = entry
                            .toString()
                            .replaceAll(File.separator, ".")
                            .replace(".class", "");

                        final String caseIgnoredClazzName = clazzName.toLowerCase();
                        if (!caseIgnoredClazzName.startsWith(CHECKERS_PACKAGE)
                            || caseIgnoredClazzName.contains("sub")
                            || caseIgnoredClazzName.contains("abstract")
                            || caseIgnoredClazzName.contains("adapter")) {
                            continue;
                        }
                        final Class<? extends Processor> clazz = jarClassLoader.loadClass(clazzName).asSubclass(superclazz);
                        if (!Modifier.isAbstract(clazz.getModifiers()) && !clazz.isAnonymousClass()) {
                            myCheckerClasses.add(clazz);
                            myState.myBuiltInCheckers.add(clazz.getCanonicalName());
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
        } catch (NoSuchMethodException e) {
            LOG.debug(e);
        } finally {
            try {
                if (jar != null) jar.close();
            } catch (IOException e) {
                LOG.error(e);
            }
        }

        refreshCheckers();
        needReload = false;
    }

    private void refreshCheckers() {
        myCheckers.clear();
        myCheckers.addAll(myState.myBuiltInCheckers);
        myCheckers.addAll(myState.myCustomCheckers);
    }

    public static class State {
        public @NotNull String myPathToCheckerJar;
        public @NotNull Set<String> myBuiltInCheckers;
        public @NotNull Set<String> myCustomCheckers;
        public @NotNull Set<String> myEnabledCheckers;
        public @NotNull List<String> myOptions;

        public State() {
            myPathToCheckerJar = "";
            myBuiltInCheckers = new HashSet<String>();
            myCustomCheckers = new HashSet<String>();
            myEnabledCheckers = new HashSet<String>();
            myOptions = new ArrayList<String>();
        }

        public State(@NotNull State state) {
            myPathToCheckerJar = state.myPathToCheckerJar;
            myBuiltInCheckers = new HashSet<String>(state.myBuiltInCheckers);
            myCustomCheckers = new HashSet<String>(state.myCustomCheckers);
            myEnabledCheckers = new HashSet<String>(state.myEnabledCheckers);
            myOptions = new ArrayList<String>(state.myOptions);
        }

        private static <T> boolean collectionEquals(@NotNull Collection<T> a, @NotNull Collection<T> b) {
            return a.containsAll(b) && b.containsAll(a);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            State state = (State) o;
            return myPathToCheckerJar.equals(state.myPathToCheckerJar)
                    && collectionEquals(myBuiltInCheckers, state.myBuiltInCheckers)
                    && collectionEquals(myCustomCheckers, state.myCustomCheckers)
                    && collectionEquals(myEnabledCheckers, state.myEnabledCheckers)
                    && collectionEquals(myOptions, state.myOptions);
        }

        @Override
        public int hashCode() {
            int result = myPathToCheckerJar.hashCode();
            result = 31 * result + myBuiltInCheckers.hashCode();
            result = 31 * result + myCustomCheckers.hashCode();
            result = 31 * result + myEnabledCheckers.hashCode();
            return result;
        }
    }
}
