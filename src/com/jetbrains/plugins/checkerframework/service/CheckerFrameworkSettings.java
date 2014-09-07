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

    private final @NotNull List<Class<? extends SourceChecker>> myEnabledCheckerClasses = new ArrayList<Class<? extends SourceChecker>>();
    private final @NotNull List<String>                         myOptions               = new ArrayList<String>();

    public @NotNull List<Class<? extends SourceChecker>> getBuiltInCheckers() {
        return CheckersStuff.BUILTIN_CHECKERS;
    }

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
            add(CheckersStuff.PATH_TO_CHECKER);
            add("-Adetailedmsgtext");
        }};
    }

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
}
