package com.jetbrains.plugins.checkerframework.configurable;

import com.intellij.openapi.components.*;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

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

    public static final String CHECKERS_BASE_CLASS = "org.checkerframework.framework.source.SourceChecker";
    public static final String CHECKERS_PACKAGE = "org.checkerframework.checker";

    private @NotNull String myPathToCheckerJar;
    private @NotNull Set<String> myEnabledCheckers;
    private Set<String> myAvailableCheckers;
    private List<Class> myAvailableCheckerClasses;
    private boolean needReload;

    @SuppressWarnings("UnusedDeclaration")
    public CheckerFrameworkSettings() {
        myPathToCheckerJar = "";
        myEnabledCheckers = new HashSet<String>();
    }

    public CheckerFrameworkSettings(@NotNull CheckerFrameworkSettings original) {
        this.loadState(original.getState());
    }

    @NotNull
    public String getPathToCheckerJar() {
        return myPathToCheckerJar;
    }

    public void setPathToCheckerJar(@NotNull String pathToCheckerJar) {
        needReload = !myPathToCheckerJar.equals(pathToCheckerJar);
        myPathToCheckerJar = pathToCheckerJar;
    }


    @NotNull
    public Set<String> getEnabledCheckers() {
        return myEnabledCheckers;
    }

    @NotNull
    public Set<String> getAvailableCheckers() {
        if (needReload) {
            getAvailableCheckerClasses();
        }
        return myAvailableCheckers;
    }

    @NotNull
    public List<Class> getAvailableCheckerClasses() {
        if (myAvailableCheckerClasses == null || needReload) {
            myAvailableCheckerClasses = new ArrayList<Class>(
                ClassScanner.findChildren(
                    new File(myPathToCheckerJar),
                    CHECKERS_BASE_CLASS, CHECKERS_PACKAGE,
                    this.getClass().getClassLoader()
                )
            );
            for (Class clazz : myAvailableCheckerClasses) {
                myAvailableCheckers.add(clazz.getCanonicalName());
            }
            needReload = false;
        }
        return myAvailableCheckerClasses;
    }

    @Nullable
    @Override
    public State getState() {
        return new State(getPathToCheckerJar(), getAvailableCheckers(), getEnabledCheckers());
    }

    @Override
    public void loadState(State state) {
        needReload = !state.myPathToCheckerJar.equals(myPathToCheckerJar);
        myPathToCheckerJar = state.myPathToCheckerJar;
        myAvailableCheckers = new HashSet<String>(state.myAvailableCheckers);
        myEnabledCheckers = new HashSet<String>(state.myEnabledCheckers);
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
        if (!myPathToCheckerJar.equals(settings.myPathToCheckerJar)) {
            return false;
        }
        if (!myAvailableCheckers.containsAll(settings.myAvailableCheckers)
            || !settings.myAvailableCheckers.containsAll(myAvailableCheckers)) {
            return false;
        }
        if (!myEnabledCheckers.containsAll(settings.myEnabledCheckers)
            || !settings.myEnabledCheckers.containsAll(myEnabledCheckers)) {
            return false;
        }
        return true;
    }

    @Override
    public int hashCode() {
        int result = myPathToCheckerJar.hashCode();
        result = 31 * result + (myAvailableCheckers.hashCode());
        result = 31 * result + (myEnabledCheckers.hashCode());
        return result;
    }

    public static CheckerFrameworkSettings getInstance(Project project) {
        return ServiceManager.getService(project, CheckerFrameworkSettings.class);
    }

    @SuppressWarnings("UnusedDeclaration")
    public static class State {
        private @NotNull String myPathToCheckerJar;
        private @NotNull Set<String> myAvailableCheckers;
        private @NotNull Set<String> myEnabledCheckers;

        public State() {
            myPathToCheckerJar = "";
            myAvailableCheckers = new HashSet<String>();
            myEnabledCheckers = new HashSet<String>();
        }

        public State(@NotNull String pathToCheckerJar,
                     @NotNull Set<String> availableCheckers,
                     @NotNull Set<String> enabledCheckers) {
            myPathToCheckerJar = pathToCheckerJar;
            myAvailableCheckers = availableCheckers;
            myEnabledCheckers = enabledCheckers;
        }

        @NotNull
        public String getPathToCheckerJar() {
            return myPathToCheckerJar;
        }

        public void setPathToCheckerJar(@NotNull String pathToCheckerJar) {
            myPathToCheckerJar = pathToCheckerJar;
        }

        @NotNull
        public Set<String> getAvailableCheckers() {
            return myAvailableCheckers;
        }

        public void setAvailableCheckers(@NotNull Set<String> availableCheckers) {
            myAvailableCheckers = availableCheckers;
        }

        @NotNull
        public Set<String> getEnabledCheckers() {
            return myEnabledCheckers;
        }

        public void setEnabledCheckers(@NotNull Set<String> enabledCheckers) {
            myEnabledCheckers = enabledCheckers;
        }
    }
}
