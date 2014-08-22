package com.jetbrains.plugins.checkerframework.service;

import com.intellij.ide.plugins.cl.PluginClassLoader;
import com.intellij.openapi.components.*;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.progress.ProcessCanceledException;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Condition;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.lang.UrlClassLoader;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import java.net.URL;
import java.util.*;
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

@SuppressWarnings("UnusedDeclaration")
@State(
    name = "CheckerFrameworkPluginSettings",
    storages = {
        @Storage(
            id = "default",
            file = StoragePathMacros.PROJECT_CONFIG_DIR + "/checkerframework-plugin-settings.xml"
        )
    }
)
public class CheckerFrameworkSettings implements PersistentStateComponent<CheckerFrameworkState> {

    private static final Logger            LOG                        = Logger.getInstance(CheckerFrameworkSettings.class);
    private static final PluginClassLoader CURRENT_PLUGIN_CLASSLOADER = (PluginClassLoader) CheckerFrameworkSettings.class.getClassLoader();

    private final @NotNull Project myProject;

    private final @NotNull List<String>   myCheckers       = new ArrayList<String>();
    private final @NotNull List<Class<?>> myCheckerClasses = new ArrayList<Class<?>>();

    private @NotNull CheckerFrameworkState myState    = new CheckerFrameworkState();
    private          boolean               needReload = true;

    private String      myErrorMessage;
    private Constructor compilerConstructor;
    private String      myPathToJavacJar;

    @SuppressWarnings("UnusedDeclaration")
    public CheckerFrameworkSettings(@NotNull Project project) {
        myProject = project;
    }

    public CheckerFrameworkSettings(@NotNull CheckerFrameworkSettings original) {
        myProject = original.myProject;
        this.loadState(original.getState());
    }

    public boolean valid() {
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

    public void addCustomChecker(@NotNull String clazzFQN) {
        myState.customCheckers.add(clazzFQN);
        refreshCheckers();
    }

    @NotNull
    public List<String> getOptions() {
        return myState.options;
    }

    @Nullable
    public Object createCompiler() {
        if (needReload) loadClasses();
        assert valid() : "Not valid";
        try {
            return compilerConstructor.newInstance(
                myProject,
                createCompileOptions(),
                getEnabledCheckerClasses()
            );
        } catch (ProcessCanceledException ignored) {
        } catch (InvocationTargetException ignored) {
        } catch (InstantiationException ignored) {
        } catch (IllegalAccessException ignored) {
        }
        return null;
    }

    @NotNull
    @Override
    public CheckerFrameworkState getState() {
        return myState;
    }

    @Override
    public void loadState(final CheckerFrameworkState state) {
        needReload = needReload || !myState.pathToCheckerJar.equals(state.pathToCheckerJar);
        myState = new CheckerFrameworkState(state);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        CheckerFrameworkSettings settings = (CheckerFrameworkSettings) o;
        return myState.equals(settings.myState);
    }

    @Override
    public int hashCode() {
        return myState.hashCode();
    }

    public static CheckerFrameworkSettings getInstance(final Project project) {
        return ServiceManager.getService(project, CheckerFrameworkSettings.class);
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

            final File checkerFile = new File(myState.pathToCheckerJar);
            if (!checkerFile.exists()) {
                myErrorMessage = "'" + myState.pathToCheckerJar + "' doesn't exist";
                return;
            } else if (checkerFile.isDirectory()) {
                myErrorMessage = "'" + myState.pathToCheckerJar + "' is a directory";
                return;
            }

            final File directory = checkerFile.getParentFile();
//            final File javacFile = new File(directory, "javac.jar");
//            if (!javacFile.exists()) {
//                myErrorMessage = "'" + javacFile.getCanonicalPath() + "' doesn't exist";
//                return;
//            } else if (javacFile.isDirectory()) {
//                myErrorMessage = "'" + javacFile.getCanonicalPath() + "' is a directory";
//                return;
//            }
//            myPathToJavacJar = javacFile.getCanonicalPath();

            final URL jarUrl = checkerFile.toURI().toURL();
//            final URL javacUrl = javacFile.toURI().toURL();

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
//                    add(javacUrl);
                }}, new ClassLoader[]{CURRENT_PLUGIN_CLASSLOADER}, CURRENT_PLUGIN_CLASSLOADER.getPluginId(), "CF", null
            );
            final Class<?> superClazz = jarClassLoader.loadClass(Stuff.CHECKERS_BASE_CLASS_FQN);

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
                        final Class<?> clazz = jarClassLoader.loadClass(clazzName).asSubclass(superClazz);
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

            for (Class checkerClass : myCheckerClasses) {
                myState.builtInCheckers.add(checkerClass.getCanonicalName());
            }

            compilerConstructor = jarClassLoader.loadClass(
                Stuff.COMPILER_IMPL_FQN
            ).getDeclaredConstructor(
                Project.class, Collection.class, Collection.class
            );

            myErrorMessage = null;
            myState.valid = true;
            CompilerHolder.getInstance(myProject).reset();
        } catch (IOException e) {
            LOG.debug(e);
            myErrorMessage = e.getMessage();
        } catch (ClassNotFoundException e) {
            LOG.debug(e);
            myErrorMessage = e.getMessage();
        } catch (NoSuchMethodException e) {
            LOG.error(e);
            myErrorMessage = "Cannot find factory method";
        } finally {
            try {
                //noinspection ConstantConditions
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

    @NotNull
    private Set<Class> getEnabledCheckerClasses() {
        if (needReload) loadClasses();
        assert valid() : "Not valid";
        final Set<Class> result = new HashSet<Class>(myCheckerClasses);
        ContainerUtil.retainAll(
            result, new Condition<Class>() {
                @Override
                public boolean value(Class aClass) {
                    return myState.enabledCheckers.contains(aClass.getCanonicalName());
                }
            }
        );
        return result;
    }

    private List<String> createCompileOptions() {
        return new ArrayList<String>(getOptions()) {{
//            add("-Xbootclasspath:" + myPathToJavacJar/* + File.pathSeparator + myState.pathToCheckerJar*/);
            add("-cp");
            add(myState.pathToCheckerJar);
            add("-Adetailedmsgtext");
        }};
    }

    private static String getAnnotatedJdkFileName() {
        return "jdk8.jar";
    }

}
