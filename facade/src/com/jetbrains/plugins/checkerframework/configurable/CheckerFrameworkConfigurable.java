package com.jetbrains.plugins.checkerframework.configurable;

import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.options.SearchableConfigurable;
import com.intellij.openapi.project.Project;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBPanel;
import com.jetbrains.plugins.checkerframework.service.CheckerFrameworkSettings;
import com.jetbrains.plugins.checkerframework.service.Stuff;
import com.jetbrains.plugins.checkerframework.util.CheckerFrameworkBundle;
import com.jetbrains.plugins.checkerframework.util.JdkVersion;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.util.Locale;

import static com.jetbrains.plugins.checkerframework.service.CheckerFrameworkState.collectionEquals;

public class CheckerFrameworkConfigurable implements SearchableConfigurable {

    private final @NotNull Project                        myProject;
    private @Nullable      CheckerFrameworkConfigurableUI myUI;

    public CheckerFrameworkConfigurable(final @NotNull Project project) {
        myProject = project;
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
        if (!JdkVersion.check()) {
            final JPanel panel = new JBPanel(new BorderLayout());
            panel.add(
                new JBLabel(
                    CheckerFrameworkBundle.message(
                        "unsupported.jre.message", String.format(Locale.US, "%.1f", JdkVersion.getJdkVersion())
                    )
                ),
                BorderLayout.NORTH
            );
            return panel;
        }
        if (myUI == null) {
            myUI = new CheckerFrameworkConfigurableUIConstructor(myProject) {
                @Override
                protected ComboBoxModel getProcessorProfileModel() {
                    return new ProcessorProfileComboboxModel(myProject);
                }
            };
        }
        return myUI.getRoot();
    }

    @Override
    public boolean isModified() {
        if (!JdkVersion.check()) return false;
        assert myUI != null;
        final CheckerFrameworkSettings mySettings = CheckerFrameworkSettings.getInstance(myProject);
        return !collectionEquals(mySettings.getEnabledCheckerClasses(), myUI.getConfiguredEnabledCheckers())
            || !collectionEquals(mySettings.getOptions(), myUI.getConfiguredOptions());
    }

    @Override
    public void apply() throws ConfigurationException {
        if (!JdkVersion.check()) return;
        assert myUI != null;
        final CheckerFrameworkSettings mySettings = CheckerFrameworkSettings.getInstance(myProject);
        mySettings.setEnabledCheckerClasses(myUI.getConfiguredEnabledCheckers());
        mySettings.setOptions(myUI.getConfiguredOptions());
    }

    @Override
    public void reset() {
        if (!JdkVersion.check()) return;
        assert myUI != null;
        myUI.reset(CheckerFrameworkSettings.getInstance(myProject));
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
