package com.jetbrains.plugins.checkerframework.service;

import com.intellij.openapi.components.*;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import org.checkerframework.framework.source.SourceChecker;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;

import static com.jetbrains.plugins.checkerframework.service.CheckerFrameworkSettings.CONFIG_PATH;

@SuppressWarnings("UnusedDeclaration")
@State(
    name = "CheckerFrameworkPluginSettings",
    storages = {
        @Storage(id = "default", file = CONFIG_PATH)
    }
)
public class CheckerFrameworkSettings implements PersistentStateComponent<CheckerFrameworkState> {
    static final         String CONFIG_PATH = StoragePathMacros.PROJECT_CONFIG_DIR + "/checkerframework-plugin-settings.xml";
    private static final Logger LOG         = Logger.getInstance(CheckerFrameworkSettings.class);
//    private static final PluginClassLoader CURRENT_PLUGIN_CLASSLOADER = (PluginClassLoader) CheckerFrameworkSettings.class.getClassLoader();

    //    private final @NotNull List<String>                         myCheckers              = new ArrayList<String>();
//    private final @NotNull List<Class<?>>                       myCheckerClasses        = new ArrayList<Class<?>>();
    private final @NotNull List<Class<? extends SourceChecker>> myEnabledCheckerClasses = new ArrayList<Class<? extends SourceChecker>>();
    private final @NotNull List<String>                         myOptions               = new ArrayList<String>();
    //    private @NotNull       CheckerFrameworkState                myState                 = new CheckerFrameworkState();
//    private                boolean                              needReload              = true;

//    @Nullable private String myErrorMessage;
//    private String      myPathToJavacJar;

    @SuppressWarnings("UnusedDeclaration")
    public CheckerFrameworkSettings() {
    }

//    public CheckerFrameworkSettings(@NotNull CheckerFrameworkSettings original) {
//        myProject = original.myProject;
//        this.loadState(original.getState());
//    }

//    public boolean valid() {
//        if (needReload) loadClasses();
//        return myState.valid;
//    }

//    @Nullable
//    public String getErrorMessage() {
//        return myErrorMessage;
//    }

//    @NotNull
//    public String getVersion() {
//        if (needReload) loadClasses();
//        return myState.version;
//    }

//    @NotNull
//    public String getPathToCheckerJar() {
//        return myState.pathToCheckerJar;
//    }

//    public void setPathToCheckerJar(@NotNull String pathToCheckerJar) {
//        needReload = !myState.pathToCheckerJar.equals(pathToCheckerJar);
//        myState.pathToCheckerJar = pathToCheckerJar;
//    }

    public @NotNull List<Class<? extends SourceChecker>> getBuiltInCheckers() {
        return Stuff.BUILTIN_CHECKERS;
//        if (needReload) loadClasses();
//        return myState.builtInCheckers;
    }

//    @NotNull
//    public Set<String> getEnabledCheckers() {
//        return myState.enabledCheckers;
//    }

//    @NotNull
//    public List<String> getCheckers() {
//        if (needReload) loadClasses();
//        return myCheckers;
//    }

//    public void addCustomChecker(@NotNull String clazzFQN) {
//        myState.customCheckers.add(clazzFQN);
//        refreshCheckers();
//    }

    public @NotNull List<Class<? extends SourceChecker>> getEnabledCheckerClasses() {
        return myEnabledCheckerClasses;
    }

    public void setEnabledCheckerClasses(Collection<Class<? extends SourceChecker>> checkerClasses) {
        myEnabledCheckerClasses.clear();
        myEnabledCheckerClasses.addAll(checkerClasses);
    }

    public @NotNull Collection<String> getOptions() {
        return myOptions;
    }

    public void setOptions(@NotNull Collection<String> options) {
        myOptions.clear();
        myOptions.addAll(options);
    }

    public @NotNull Collection<String> createCompilerOptions() {
        return new LinkedHashSet<String>(myOptions) {{
            add("-cp");
            add(Stuff.PATH_TO_CHECKER);
            add("-Adetailedmsgtext");
        }};
    }

//    @NotNull
//    public Object createCompiler() throws IllegalAccessException, InvocationTargetException, InstantiationException {
//        if (needReload) loadClasses();
//        assert valid() : "Not valid";
//        return ApplicationManager.getApplication().executeOnPooledThread(new Callable<Object>() {
//            @Override
//            public Object call() throws Exception {
//                return compilerConstructor.newInstance(
//                    myProject,
//                    createCompileOptions(),
//                    getEnabledCheckerClasses()
//                );
//            }
//        });
//    }

    @Override
    public @NotNull CheckerFrameworkState getState() {
        final CheckerFrameworkState state = new CheckerFrameworkState();
        for (Class<? extends SourceChecker> clazz : myEnabledCheckerClasses) {
            state.enabledCheckers.add(clazz.getCanonicalName());
        }
        state.options.addAll(myOptions);
        return state;
    }

    @Override
    public void loadState(@NotNull final CheckerFrameworkState state) {
        myEnabledCheckerClasses.clear();
        for (String checkerFQN : state.enabledCheckers) {
            try {
                myEnabledCheckerClasses.add(Class.forName(checkerFQN).asSubclass(SourceChecker.class));
            } catch (ClassNotFoundException e) {
                LOG.error(e);
                throw new RuntimeException(e);
            } catch (ClassCastException e) {
                LOG.error(e);
                throw e;
            }
        }
        myOptions.clear();
        myOptions.addAll(state.options);
    }

    public static CheckerFrameworkSettings getInstance(@NotNull final Project project) {
        return ServiceManager.getService(project, CheckerFrameworkSettings.class);
    }
/*
    private void clean() {
        myCheckerClasses.clear();
        myState.builtInCheckers.clear();
        myState.valid = false;
//        compilerConstructor = null;
    }

    private void loadClasses() {
        clean();

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
            final List<Class<?>> enabledClasses = new ArrayList<Class<?>>();
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
                            if (myState.enabledCheckers.contains(clazz.getCanonicalName())) {
                                enabledClasses.add(clazz);
                            }
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

            final Constructor compilerConstructor = jarClassLoader.loadClass(
                Stuff.COMPILER_IMPL_FQN
            ).getDeclaredConstructor(
                Project.class, Collection.class, Collection.class
            );

            CompilerHolder.getInstance(myProject).reset(
                compilerConstructor,
                createCompileOptions(),
                enabledClasses
            );

            myErrorMessage = null;
            myState.valid = true;
        } catch (IOException e) {
            LOG.debug(e);
            myErrorMessage = e.getMessage();
            clean();
        } catch (ClassNotFoundException e) {
            LOG.debug(e);
            myErrorMessage = e.getMessage();
            clean();
        } catch (NoSuchMethodException e) {
            LOG.error(e);
            myErrorMessage = "Cannot find factory method";
            clean();
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
*/
//    @NotNull
//    public Set<Class> getEnabledCheckerClasses() {
//        if (needReload) loadClasses();
//        assert valid() : "Not valid";
//        final Set<Class> result = new HashSet<Class>(myCheckerClasses);
//        ContainerUtil.retainAll(
//            result, new Condition<Class>() {
//                @Override
//                public boolean value(Class aClass) {
//                    return myState.enabledCheckers.contains(aClass.getCanonicalName());
//                }
//            }
//        );
//        return result;
//    }
//


//    @NotNull
//    public List<String> createCompileOptions() {
//        return new ArrayList<String>(getOptions()) {{
//            add("-Xbootclasspath:" + myPathToJavacJar/* + File.pathSeparator + myState.pathToCheckerJar*/);
//            add("-cp");
//            add(myState.pathToCheckerJar);
//            add("-Adetailedmsgtext");
//        }};
//    }
}
