package com.jetbrains.plugins.checkerframework.configurable;

import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.project.Project;
import com.intellij.ui.DocumentAdapter;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableModel;
import java.io.File;

public class CheckerFrameworkConfigurable implements Configurable {

    private final CheckerFrameworkSettings myOriginalSettings;
    private final CheckerFrameworkSettings mySettings;
    private CheckerFrameworkConfigurableUI myUI;
    private CheckersTableModel myCheckersTableModel;

    public CheckerFrameworkConfigurable(final Project project) {
        myOriginalSettings = CheckerFrameworkSettings.getInstance(project);
        mySettings = new CheckerFrameworkSettings(myOriginalSettings);
        myCheckersTableModel = new CheckersTableModel();
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
        return getUI().getRoot();
    }

    @Override
    public boolean isModified() {
        return !(myOriginalSettings.equals(mySettings));
    }

    @Override
    public void apply() throws ConfigurationException {
        mySettings.getEnabledCheckers().retainAll(mySettings.getAvailableCheckers());
        myOriginalSettings.loadState(mySettings.getState());
    }

    @Override
    public void reset() {
        mySettings.loadState(myOriginalSettings.getState());
        getUI().setPathToCheckerJar(mySettings.getPathToCheckerJar());
    }

    @Override
    public void disposeUIResources() {
        // nothing to do here
    }

    private CheckerFrameworkConfigurableUI getUI() {
        if (myUI == null) {
            final PathToJarChangeListener pathToJarChangeListener = new PathToJarChangeListener();
            myUI = new CheckerFrameworkConfigurableUI() {
                @Override
                protected TableModel getCheckersModel() {
                    return myCheckersTableModel;
                }

                @Override
                protected DocumentListener getPathToJarChangeListener() {
                    return pathToJarChangeListener;
                }
            };
        }
        return myUI;
    }

    private class PathToJarChangeListener extends DocumentAdapter {

        @Override
        protected void textChanged(DocumentEvent e) {
            // clean warning state
            final CheckerFrameworkConfigurableUI ui = getUI();
            ui.hideWarning();

            // check if jar exists
            final String pathToCheckerJar = getUI().getPathToCheckerJar();
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

    private class CheckersTableModel extends AbstractTableModel {

        private final String[] myColumnNames = {"Enabled/Disabled", "Checker class"};

        @Override
        public int getColumnCount() {
            return myColumnNames.length;
        }

        @Override
        public String getColumnName(int col) {
            return myColumnNames[col];
        }

        @Override
        public int getRowCount() {
            return mySettings.getAvailableCheckerClasses().size();
        }

        @Override
        public Object getValueAt(int row, int col) {
            final Class clazz = mySettings.getAvailableCheckerClasses().get(row);
            return col == 0 ? mySettings.getEnabledCheckers().contains(clazz.getCanonicalName()) : clazz;
        }

        @Override
        public Class getColumnClass(int c) {
            switch (c) {
                case 0:
                    return Boolean.class;
                default:
                    return Class.class;
            }
        }

        @Override
        public boolean isCellEditable(int row, int col) {
            return col == 0;
        }

        @Override
        public void setValueAt(Object value, int row, int col) {
            final Class clazz = mySettings.getAvailableCheckerClasses().get(row);
            if (Boolean.TRUE.equals(value)) {
                mySettings.getEnabledCheckers().add(clazz.getCanonicalName());
            } else {
                mySettings.getEnabledCheckers().remove(clazz.getCanonicalName());
            }
            fireTableCellUpdated(row, col);
        }
    }
}
