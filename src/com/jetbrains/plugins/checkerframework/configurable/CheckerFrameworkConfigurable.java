package com.jetbrains.plugins.checkerframework.configurable;

import com.intellij.ide.util.ClassFilter;
import com.intellij.ide.util.TreeClassChooser;
import com.intellij.ide.util.TreeClassChooserFactory;
import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiClass;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.ui.AnActionButton;
import com.intellij.ui.AnActionButtonRunnable;
import com.intellij.ui.DocumentAdapter;
import com.intellij.ui.UIBundle;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.TableModel;
import java.io.File;

public class CheckerFrameworkConfigurable implements Configurable {

    private final Project myProject;
    private final CheckerFrameworkSettings myOriginalSettings;
    private final CheckerFrameworkSettings mySettings;
    private final PathToJarChangeListener pathToJarChangeListener;
    private final CheckersTableModel myCheckersTableModel;
    private final OptionsTableModel myOptionsTableModel;
    private final AddCustomCheckerListener myAddCustomCheckerListener;
    private final CheckerFrameworkConfigurableUI myUI;

    public CheckerFrameworkConfigurable(final Project project) {
        myProject = project;
        myOriginalSettings = CheckerFrameworkSettings.getInstance(project);
        mySettings = new CheckerFrameworkSettings(myOriginalSettings);
        pathToJarChangeListener = new PathToJarChangeListener();
        myCheckersTableModel = new CheckersTableModel(mySettings);
        myOptionsTableModel = new OptionsTableModel(mySettings.getOptions());
        myAddCustomCheckerListener = new AddCustomCheckerListener();
        myUI = new CheckerFrameworkConfigurableUI() {
            @Override
            protected DocumentListener getPathToJarChangeListener() {
                return pathToJarChangeListener;
            }

            @Override
            protected TableModel getCheckersModel() {
                return myCheckersTableModel;
            }

            @Override
            protected TableModel getOptionsModel() {
                return myOptionsTableModel;
            }

            @Override
            protected AnActionButtonRunnable getAddCustomCheckerHandler() {
                return myAddCustomCheckerListener;
            }
        };
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
        myOptionsTableModel.setData(mySettings.getOptions());
        myUI.setPathToCheckerJar(mySettings.getPathToCheckerJar());
    }

    @Override
    public void disposeUIResources() {
        // nothing to do here
    }

    private class PathToJarChangeListener extends DocumentAdapter {

        @Override
        protected void textChanged(DocumentEvent e) {
            // clean warning state
            final CheckerFrameworkConfigurableUI ui = myUI;
            ui.hideWarning();

            // check if jar exists
            final String pathToCheckerJar = myUI.getPathToCheckerJar();
            final File checkerJar = new File(pathToCheckerJar);
            if (!checkerJar.exists()) {
                ui.showWarning("'" + pathToCheckerJar + "' doesn't exist");
            } else if (checkerJar.isDirectory()) {
                ui.showWarning("'" + pathToCheckerJar + "' is a directory");
            }

            mySettings.setPathToCheckerJar(pathToCheckerJar);
            myCheckersTableModel.fireTableDataChanged();
        }
    }

    private class AddCustomCheckerListener implements AnActionButtonRunnable {
        @Override
        public void run(AnActionButton button) {
            final TreeClassChooser chooser = TreeClassChooserFactory.getInstance(myProject).createNoInnerClassesScopeChooser(
                UIBundle.message("class.filter.editor.choose.class.title"),
                GlobalSearchScope.allScope(myProject),
                ClassFilter.ALL,
                null
            );
            chooser.showDialog();
            final PsiClass selectedClass = chooser.getSelected();
            if (selectedClass != null) {
                final String fqn = selectedClass.getQualifiedName();
                if (fqn != null) {
                    mySettings.addCustomChecker(fqn);
                }
                myCheckersTableModel.fireTableDataChanged();
            }
        }
    }
}
