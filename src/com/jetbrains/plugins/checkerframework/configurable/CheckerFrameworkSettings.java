package com.jetbrains.plugins.checkerframework.configurable;

import com.intellij.openapi.components.*;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;


@State(
    name = "CheckerFrameworkPluginSettings",
    storages = {
        @Storage(
            id = "default",
            file = StoragePathMacros.PROJECT_CONFIG_DIR + "/checkerframework-plugin-settings.xml"
        )
    }
)
public class CheckerFrameworkSettings implements PersistentStateComponent<CheckerFrameworkSettings> {

    private List<String> myActiveCheckers;

    public CheckerFrameworkSettings() {
        this.myActiveCheckers = new ArrayList<String>();
    }

    @Nullable
    @Override
    public CheckerFrameworkSettings getState() {
        return this;
    }

    @Override
    public void loadState(@NotNull CheckerFrameworkSettings state) {
        this.myActiveCheckers = state.myActiveCheckers;
    }

    public List<String> getActiveCheckers() {
        return myActiveCheckers;
    }

    public void setActiveCheckers(List<String> activeCheckers) {
        this.myActiveCheckers = activeCheckers;
    }

    @NotNull
    public static CheckerFrameworkSettings getInstance(@NotNull Project project) {
        return ServiceManager.getService(project, CheckerFrameworkSettings.class);
    }
}
