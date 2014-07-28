package com.jetbrains.plugins.checkerframework.configurable;

import com.intellij.openapi.components.*;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.*;

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

    private @NotNull State myState = new State();
    private List<String> myAllCheckers = null;
    private List<Class> myAvailableCheckerClasses = null;
    private boolean needReload = true;

    @SuppressWarnings("UnusedDeclaration")
    public CheckerFrameworkSettings() {
    }

    public CheckerFrameworkSettings(@NotNull CheckerFrameworkSettings original) {
        this.loadState(original.getState());
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
    public Set<String> getEnabledCheckers() {
        return myState.myEnabledCheckers;
    }

    @NotNull
    public List<String> getAllCheckers() {
        if (needReload) {
            loadClasses();
        }
        return myAllCheckers;
    }

    @NotNull
    public Set<String> getAvailableCheckers() {
        if (needReload) {
            loadClasses();
        }
        return myState.myAvailableCheckers;
    }

    @SuppressWarnings("UnusedDeclaration")
    @NotNull
    public List<Class> getAvailableCheckerClasses() {
        if (needReload) {
            loadClasses();
        }
        return myAvailableCheckerClasses;
    }

    @NotNull
    List<String> getOptions() {
        return myState.myOptions;
    }

    @NotNull
    @Override
    public State getState() {
        return myState;
    }

    @Override
    public void loadState(final State state) {
        if (!needReload) {
            needReload = !state.myPathToCheckerJar.equals(state.myPathToCheckerJar);
        }
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

    private void loadClasses() {
        myAvailableCheckerClasses = new ArrayList<Class>(
            ClassScanner.findChildren(
                new File(myState.myPathToCheckerJar),
                CHECKERS_BASE_CLASS, CHECKERS_PACKAGE,
                this.getClass().getClassLoader()
            )
        );
        myState.myAvailableCheckers.clear();
        for (final Class clazz : myAvailableCheckerClasses) {
            myState.myAvailableCheckers.add(clazz.getCanonicalName());
        }
        myAllCheckers = new ArrayList<String>();
        myAllCheckers.addAll(myState.myAvailableCheckers);
        myAllCheckers.addAll(myState.myCustomCheckers);
        needReload = false;
    }

    public static CheckerFrameworkSettings getInstance(final Project project) {
        return ServiceManager.getService(project, CheckerFrameworkSettings.class);
    }

    public static class State {
        public @NotNull String myPathToCheckerJar;
        public @NotNull Set<String> myAvailableCheckers;
        public @NotNull Set<String> myCustomCheckers;
        public @NotNull Set<String> myEnabledCheckers;
        public @NotNull List<String> myOptions;

        public State() {
            myPathToCheckerJar = "";
            myAvailableCheckers = new HashSet<String>();
            myCustomCheckers = new HashSet<String>();
            myEnabledCheckers = new HashSet<String>();
            myOptions = new ArrayList<String>();
        }

        public State(@NotNull State state) {
            myPathToCheckerJar = state.myPathToCheckerJar;
            myAvailableCheckers = new HashSet<String>(state.myAvailableCheckers);
            myCustomCheckers = new HashSet<String>(state.myCustomCheckers);
            myEnabledCheckers = new HashSet<String>(state.myEnabledCheckers);
            myOptions = new ArrayList<String>(state.myOptions);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            State state = (State)o;
            if (!myPathToCheckerJar.equals(state.myPathToCheckerJar)) {
                return false;
            }
            if (!collectionEquals(myEnabledCheckers, state.myEnabledCheckers)) {
                return false;
            }
            if (!collectionEquals(myCustomCheckers, state.myCustomCheckers)) {
                return false;
            }
            if (!collectionEquals(myAvailableCheckers, state.myAvailableCheckers)) {
                return false;
            }
            if (!collectionEquals(myOptions, state.myOptions)) {
                return false;
            }
            return true;
        }

        @Override
        public int hashCode() {
            int result = myPathToCheckerJar.hashCode();
            result = 31 * result + myAvailableCheckers.hashCode();
            result = 31 * result + myEnabledCheckers.hashCode();
            return result;
        }

        private static <T> boolean collectionEquals(@NotNull Collection<T> a, @NotNull Collection<T> b) {
            return a.containsAll(b) && b.containsAll(a);
        }
    }
}
