package com.jetbrains.plugins.checkerframework.configurable;

/**
 * @author Daniil Ovchinnikov.
 * @since 7/8/14.
 */

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
public class Settings implements PersistentStateComponent<Settings> {

    private List<String> activeCheckers;

    public Settings() {
        this.activeCheckers = new ArrayList<String>();
    }

    @Nullable
    @Override
    public Settings getState() {
        return this;
    }

    @Override
    public void loadState(@NotNull Settings state) {
        this.activeCheckers = state.activeCheckers;
    }

    public List<String> getActiveCheckers() {
        return activeCheckers;
    }

    public void setActiveCheckers(List<String> activeCheckers) {
        this.activeCheckers = activeCheckers;
    }

    @NotNull
    public static Settings getInstance(@NotNull Project project) {
        return ServiceManager.getService(project, Settings.class);
    }
}
