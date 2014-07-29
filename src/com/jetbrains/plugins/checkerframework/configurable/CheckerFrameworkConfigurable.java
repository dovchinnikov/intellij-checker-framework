package com.jetbrains.plugins.checkerframework.configurable;

import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

public class CheckerFrameworkConfigurable implements Configurable {

    private final CheckerFrameworkSettings myOriginalSettings;
    private final CheckerFrameworkSettings mySettings;
    private final CheckerFrameworkConfigurableUI myUI;

    public CheckerFrameworkConfigurable(final @NotNull Project project) {
        myOriginalSettings = CheckerFrameworkSettings.getInstance(project);
        mySettings = new CheckerFrameworkSettings(myOriginalSettings);
        myUI = new CheckerFrameworkConfigurableUI(project, mySettings);
    }

    @Nls
    @Override
    public String getDisplayName() {
        return "Checker Framework";
    }

    @Nullable
    @Override
    public String getHelpTopic() {
        return null;
    }

    @Nullable
    @Override
    public JComponent createComponent() {
        return myUI.getRoot();
    }

    @Override
    public boolean isModified() {
        return !(myOriginalSettings.equals(mySettings));
    }

    @Override
    public void apply() throws ConfigurationException {
        mySettings.getEnabledCheckers().retainAll(mySettings.getCheckers());
        myOriginalSettings.loadState(mySettings.getState());
    }

    @Override
    public void reset() {
        mySettings.loadState(myOriginalSettings.getState());
        myUI.reset();
    }

    @Override
    public void disposeUIResources() {
        // nothing to do here
    }
}
