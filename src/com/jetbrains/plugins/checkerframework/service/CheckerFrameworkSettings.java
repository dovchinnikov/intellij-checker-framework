package com.jetbrains.plugins.checkerframework.service;

import com.intellij.ide.plugins.cl.PluginClassLoader;
import com.intellij.openapi.components.*;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Condition;
import com.intellij.openapi.util.text.StringUtil;
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
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

@State(
    name = "CheckerFrameworkPluginSettings",
    storages = {
        @Storage(
            id = "default",
            file = StoragePathMacros.PROJECT_CONFIG_DIR + "/checkerframework-plugin-settings.xml"
        )
    }
)
public class CheckerFrameworkSettings implements PersistentStateComponent<CheckerFrameworkSettingsState> {

    private static final Logger            LOG                        = Logger.getInstance(CheckerFrameworkSettings.class);
    private static final PluginClassLoader CURRENT_PLUGIN_CLASSLOADER = (PluginClassLoader)CheckerFrameworkSettings.class.getClassLoader();

    private final @NotNull List<String>                     myCheckers       = new ArrayList<String>();
    private final @NotNull List<Class<? extends Processor>> myCheckerClasses = new ArrayList<Class<? extends Processor>>();

    private @NotNull CheckerFrameworkSettingsState myState = new CheckerFrameworkSettingsState();

    private boolean needReload = true;
    private Method myAggregateProcessorFactoryMethod;
    private String myErrorMessage;

    @SuppressWarnings("UnusedDeclaration")
    public CheckerFrameworkSettings() {
    }

    public CheckerFrameworkSettings(@NotNull CheckerFrameworkSettings original) {
        this.loadState(original.getState());
    }

    public static CheckerFrameworkSettings getInstance(final Project project) {
        return ServiceManager.getService(project, CheckerFrameworkSettings.class);
    }

    public boolean isValid() {
        if (needReload) loadClasses();
        return myState.valid;
    }

    public String getErrorMessage() {
        return myErrorMessage;
    }

    @NotNull
    public String getVersion() {
        if (needReload) loadClasses();
        return myState.version;
    }

    @NotNull
    public String getPathToCheckerJar() {
        return myState.pathToCheckerJar;
    }

    public void setPathToCheckerJar(@NotNull String pathToCheckerJar) {
        needReload = !myState.pathToCheckerJar.equals(pathToCheckerJar);
        myState.pathToCheckerJar = pathToCheckerJar;
    }

    @NotNull
    public Set<String> getBuiltInCheckers() {
        if (needReload) loadClasses();
        return myState.builtInCheckers;
    }

    @NotNull
    public Set<String> getEnabledCheckers() {
        return myState.enabledCheckers;
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
        ContainerUtil.retainAll(
            result, new Condition<Class<? extends Processor>>() {
                @Override
                public boolean value(Class<? extends Processor> aClass) {
                    return myState.enabledCheckers.contains(aClass.getCanonicalName());
                }
            }
        );
        return result;
    }

    public void addCustomChecker(@NotNull String clazzFQN) {
        myState.customCheckers.add(clazzFQN);
        refreshCheckers();
    }

    @NotNull
    public List<String> getOptions() {
        return myState.options;
    }

    @NotNull
    @Override
    public CheckerFrameworkSettingsState getState() {
        return myState;
    }

    @Override
    public void loadState(final CheckerFrameworkSettingsState state) {
        needReload = needReload || !state.pathToCheckerJar.equals(state.pathToCheckerJar);
        myState = new CheckerFrameworkSettingsState(state);
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
        myState.builtInCheckers.clear();
        myState.valid = false;

        JarFile jar = null;
        try {
            if (StringUtil.isEmptyOrSpaces(myState.pathToCheckerJar)) {
                myErrorMessage = "Path to checker.jar is not selected";
                return;
            }

            final File file = new File(myState.pathToCheckerJar);
            if (!file.exists()) {
                myErrorMessage = "'" + myState.pathToCheckerJar + "' doesn't exist";
                return;
            } else if (file.isDirectory()) {
                myErrorMessage = "'" + myState.pathToCheckerJar + "' is a directory";
                return;
            }

            final URL jarUrl = new File(myState.pathToCheckerJar).toURI().toURL();
            //noinspection IOResourceOpenedButNotSafelyClosed
            jar = new JarFile(jarUrl.getFile());
            {
                final Manifest manifest = jar.getManifest();
                final Attributes attributes = manifest.getMainAttributes();
                final String version = attributes.getValue(Stuff.MANIFEST_VERSION_KEY);
                if (StringUtil.isEmptyOrSpaces(version)) {
                    myErrorMessage = "Cannot determine version";
                    return;
                }
                if (version.compareTo(Stuff.MINIMUM_SUPPORTED_VERSION) < 0) {
                    myErrorMessage = "Version required: " + Stuff.MINIMUM_SUPPORTED_VERSION + ", found: " + version;
                    return;
                }
                myState.version = version;
            }

            final UrlClassLoader jarClassLoader = new PluginClassLoader(
                new ArrayList<URL>(CURRENT_PLUGIN_CLASSLOADER.getUrls()) {{
                    add(jarUrl);
                }}, new ClassLoader[]{CURRENT_PLUGIN_CLASSLOADER}, CURRENT_PLUGIN_CLASSLOADER.getPluginId(), "CF", null
            );
            final Class<? extends Processor> superclazz =
                jarClassLoader.loadClass(Stuff.CHECKERS_BASE_CLASS_FQN).asSubclass(Processor.class);
            final Class<?> aggregateProcessorClass =
                jarClassLoader.loadClass(Stuff.AGGREGATE_PROCESSOR_FQN);

            try {
                myAggregateProcessorFactoryMethod = aggregateProcessorClass.getDeclaredMethod("create", Collection.class);
            } catch (NoSuchMethodException e) {
                LOG.error(e);
                myErrorMessage = "Cannot find factory method";
                return;
            }

            for (final JarEntry entry : Collections.list(jar.entries())) {
                if (!entry.isDirectory() && entry.toString().endsWith(".class")) {
                    try {
                        @NonNls final String clazzName = entry
                            .toString()
                            .replaceAll(File.separator, ".")
                            .replace(".class", "");

                        final String caseIgnoredClazzName = clazzName.toLowerCase();
                        if (!caseIgnoredClazzName.startsWith(Stuff.CHECKERS_PACKAGE)
                            || caseIgnoredClazzName.contains("sub")
                            || caseIgnoredClazzName.contains("abstract")
                            || caseIgnoredClazzName.contains("adapter")) {
                            continue;
                        }
                        final Class<? extends Processor> clazz = jarClassLoader.loadClass(clazzName).asSubclass(superclazz);
                        if (!Modifier.isAbstract(clazz.getModifiers()) && !clazz.isAnonymousClass()) {
                            myCheckerClasses.add(clazz);
                            myState.builtInCheckers.add(clazz.getCanonicalName());
                        }
                    } catch (ClassNotFoundException e) {
                        LOG.error(e);
                    } catch (ClassCastException e) {
                        LOG.debug(e);
                    }
                }
            }
            myErrorMessage = null;
            myState.valid = true;
        } catch (IOException e) {
            LOG.debug(e);
            myErrorMessage = e.getMessage();
        } catch (ClassNotFoundException e) {
            LOG.debug(e);
            myErrorMessage = e.getMessage();
        } finally {
            try {
                if (jar != null) jar.close();
            } catch (IOException e) {
                LOG.error(e);
            }
            refreshCheckers();
            needReload = false;
        }
    }

    private void refreshCheckers() {
        myCheckers.clear();
        myCheckers.addAll(myState.builtInCheckers);
        myCheckers.addAll(myState.customCheckers);
    }
}
