package com.jetbrains.plugins.checkerframework.service;

import org.jetbrains.annotations.NotNull;

import java.util.*;

public class CheckerFrameworkState {
    public @NotNull String       pathToCheckerJar;
    public @NotNull Set<String>  builtInCheckers;
    public @NotNull Set<String>  customCheckers;
    public @NotNull Set<String>  enabledCheckers;
    public @NotNull List<String> options;
    public @NotNull String       version;
    public          boolean      valid;

    public CheckerFrameworkState() {
        pathToCheckerJar = "";
        builtInCheckers = new HashSet<String>();
        customCheckers = new HashSet<String>();
        enabledCheckers = new HashSet<String>();
        options = new ArrayList<String>();
        version = "";
        valid = false;
    }

    public CheckerFrameworkState(@NotNull CheckerFrameworkState state) {
        pathToCheckerJar = state.pathToCheckerJar;
        builtInCheckers = new HashSet<String>(state.builtInCheckers);
        customCheckers = new HashSet<String>(state.customCheckers);
        enabledCheckers = new HashSet<String>(state.enabledCheckers);
        options = new ArrayList<String>(state.options);
        version = state.version;
        valid = state.valid;
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
        final CheckerFrameworkState state = (CheckerFrameworkState)o;
        return valid == state.valid
               && version.equals(version)
               && pathToCheckerJar.equals(state.pathToCheckerJar)
               && collectionEquals(builtInCheckers, state.builtInCheckers)
               && collectionEquals(customCheckers, state.customCheckers)
               && collectionEquals(enabledCheckers, state.enabledCheckers)
               && collectionEquals(options, state.options);
    }

    @Override
    public int hashCode() {
        int result = pathToCheckerJar.hashCode();
        result = 31 * result + builtInCheckers.hashCode();
        result = 31 * result + customCheckers.hashCode();
        result = 31 * result + enabledCheckers.hashCode();
        result = 31 * result + options.hashCode();
        result = 31 * result + version.hashCode();
        result = 31 * result + (valid ? 1 : 0);
        return result;
    }
}
