package com.jetbrains.plugins.checkerframework.configurable;

import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.project.Project;
import org.checkerframework.javacutil.AbstractTypeProcessor;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableModel;
import java.util.HashSet;
import java.util.Set;

public class CheckerFrameworkConfigurable implements Configurable {

    private final Set<String> myToBeEnabledProcessors = new HashSet<String>();
    private final Set<String> myToBeDisabledProcessors = new HashSet<String>();

    private CheckerFrameworkConfigurableUI myUI;
    private CheckerFrameworkSettings mySettings;

    public CheckerFrameworkConfigurable(final Project project) {
        mySettings = CheckerFrameworkSettings.getInstance(project);
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
        return !(
            myToBeEnabledProcessors.isEmpty()
            && myToBeDisabledProcessors.isEmpty()
            && mySettings.getPathToCheckerJar().equals(getUI().getPathToCheckerJar())
        );
    }

    @Override
    public void apply() throws ConfigurationException {
        mySettings.getEnabledCheckers().addAll(myToBeEnabledProcessors);
        mySettings.getEnabledCheckers().removeAll(myToBeDisabledProcessors);
        mySettings.setPathToCheckerJar(getUI().getPathToCheckerJar());
        reset();
    }

    @Override
    public void reset() {
        myToBeEnabledProcessors.clear();
        myToBeDisabledProcessors.clear();
        getUI().setPathToCheckerJar(mySettings.getPathToCheckerJar());
    }

    @Override
    public void disposeUIResources() {
        // nothing to do here
    }

    private CheckerFrameworkConfigurableUI getUI() {
        if (myUI == null) {
            final TableModel myCheckersModel = new CheckersTableModel();
            myUI = new CheckerFrameworkConfigurableUI() {
                @Override
                protected TableModel getCheckersModel() {
                    return myCheckersModel;
                }
            };
        }
        return myUI;
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
            return CheckerFrameworkSettings.BUILTIN_CHECKERS.size();
        }

        @Override
        public Object getValueAt(int row, int col) {
            final Class clazz = CheckerFrameworkSettings.BUILTIN_CHECKERS.get(row);
            final String canonicalName = clazz.getCanonicalName();
            return col == 0
                   ? (mySettings.getEnabledCheckers().contains(canonicalName) || myToBeEnabledProcessors.contains(canonicalName))
                     && !myToBeDisabledProcessors.contains(canonicalName)
                   : clazz;
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
            if (value.equals(getValueAt(row, col))) {
                return;
            }

            final Class<? extends AbstractTypeProcessor> clazz = CheckerFrameworkSettings.BUILTIN_CHECKERS.get(row);
            final Set<String> setToAddTo = Boolean.TRUE.equals(value) ? myToBeEnabledProcessors : myToBeDisabledProcessors;
            final Set<String> setToRemoveFrom = Boolean.TRUE.equals(value) ? myToBeDisabledProcessors : myToBeEnabledProcessors;

            if (!setToRemoveFrom.remove(clazz.getCanonicalName())) {
                setToAddTo.add(clazz.getCanonicalName());
            }

            fireTableCellUpdated(row, col);
        }
    }
}
