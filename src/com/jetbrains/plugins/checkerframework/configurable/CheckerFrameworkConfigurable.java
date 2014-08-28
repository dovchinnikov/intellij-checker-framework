package com.jetbrains.plugins.checkerframework.configurable;

import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.options.SearchableConfigurable;
import com.intellij.openapi.project.Project;
import com.jetbrains.plugins.checkerframework.service.CheckerFrameworkSettings;
import com.jetbrains.plugins.checkerframework.service.CompilerHolder;
import com.jetbrains.plugins.checkerframework.service.Stuff;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

import static com.jetbrains.plugins.checkerframework.service.CheckerFrameworkState.collectionEquals;

public class CheckerFrameworkConfigurable implements SearchableConfigurable {

    private final @NotNull Project                        myProject;
    private final @NotNull CheckerFrameworkSettings       mySettings;
    private @Nullable      CheckerFrameworkConfigurableUI myUI;

    public CheckerFrameworkConfigurable(final @NotNull Project project) {
        myProject = project;
        mySettings = CheckerFrameworkSettings.getInstance(project);
    }

    @Nls
    @Override
    public @NotNull String getDisplayName() {
        return "Checker Framework";
    }

    @Override
    public @Nullable String getHelpTopic() {
        return null;
    }

    @Override
    public @Nullable JComponent createComponent() {
        if (myUI == null) {
            myUI = new CheckerFrameworkConfigurableUI(mySettings);
        }
        return myUI.getRoot();
    }

    @Override
    public boolean isModified() {
        assert myUI != null;
        return !collectionEquals(mySettings.getEnabledCheckerClasses(), myUI.getConfiguredEnabledCheckers())
            || !collectionEquals(mySettings.getOptions(), myUI.getConfiguredOptions());
    }

    @Override
    public void apply() throws ConfigurationException {
        assert myUI != null;
        mySettings.setEnabledCheckerClasses(myUI.getConfiguredEnabledCheckers());
        mySettings.setOptions(myUI.getConfiguredOptions());
        CompilerHolder.getInstance(myProject).resetAsync();
    }

    @Override
    public void reset() {
        assert myUI != null;
        myUI.reset(mySettings);
    }

    @Override
    public void disposeUIResources() {
        // nothing to do here
    }

    @Override
    public @NotNull String getId() {
        return Stuff.CONFIGURABLE_ID;
    }

    @Override
    public @Nullable Runnable enableSearch(String option) {
        return null;
    }
}
