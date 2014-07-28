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
    private final PathToJarChangeListener pathToJarChangeListener;
    private final CheckersTableModel myCheckersTableModel;
    private final OptionsTableModel myOptionsTableModel;
    private final CheckerFrameworkConfigurableUI myUI;

    public CheckerFrameworkConfigurable(final Project project) {
        myOriginalSettings = CheckerFrameworkSettings.getInstance(project);
        mySettings = new CheckerFrameworkSettings(myOriginalSettings);
        pathToJarChangeListener = new PathToJarChangeListener();
        myCheckersTableModel = new CheckersTableModel();
        myOptionsTableModel = new OptionsTableModel(mySettings.getOptions());
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
        mySettings.getEnabledCheckers().retainAll(mySettings.getAvailableCheckers());
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

    private class CheckersTableModel extends AbstractTableModel {

        private final String[] myColumnNames = {"Enabled", "Checker class"};
        private final Class[] myColumnClasses = {Boolean.class, String.class};

        @Override
        public int getColumnCount() {
            return myColumnNames.length;
        }

        @Override
        public String getColumnName(int column) {
            return myColumnNames[column];
        }

        @Override
        public Class getColumnClass(int column) {
            return myColumnClasses[column];
        }

        @Override
        public int getRowCount() {
            return mySettings.getAllCheckers().size();
        }

        @Override
        public boolean isCellEditable(int row, int col) {
            return col == 0;
        }

        @Override
        public Object getValueAt(int row, int col) {
            final String clazzName = mySettings.getAllCheckers().get(row);
            return col == 0 ? mySettings.getEnabledCheckers().contains(clazzName) : clazzName;
        }

        @Override
        public void setValueAt(Object value, int row, int col) {
            final String clazzName = mySettings.getAllCheckers().get(row);
            if (Boolean.TRUE.equals(value)) {
                mySettings.getEnabledCheckers().add(clazzName);
            } else {
                mySettings.getEnabledCheckers().remove(clazzName);
            }
            fireTableCellUpdated(row, col);
        }
    }
}
