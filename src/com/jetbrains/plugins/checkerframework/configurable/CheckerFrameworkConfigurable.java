package com.jetbrains.plugins.checkerframework.configurable;

import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.project.Project;
import com.intellij.ui.table.JBTable;
import org.checkerframework.javacutil.AbstractTypeProcessor;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.table.AbstractTableModel;
import java.util.HashSet;
import java.util.Set;

/**
 * @author Daniil Ovchinnikov.
 * @since 7/8/14.
 */
public class CheckerFrameworkConfigurable implements Configurable {

    private JPanel myRootPane;
    private JBTable myAvailableCheckersTable;

    private final Project myProject;
    private Set<String> myActiveCheckers;
    private Set<String> mySavedActiveCheckers;

    public CheckerFrameworkConfigurable(@NotNull Project project) {
        myProject = project;
        reset();
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
        return myRootPane;
    }

    @Override
    public boolean isModified() {
        return !(
            mySavedActiveCheckers.size() == myActiveCheckers.size()
            && mySavedActiveCheckers.containsAll(myActiveCheckers)
            && myActiveCheckers.containsAll(mySavedActiveCheckers)
        );
    }

    @Override
    public void apply() throws ConfigurationException {
        CheckerFrameworkSettings.getInstance(myProject).setActiveCheckers(myActiveCheckers);
        mySavedActiveCheckers = new HashSet<String>(myActiveCheckers);
    }

    @Override
    public void reset() {
        myActiveCheckers = new HashSet<String>(CheckerFrameworkSettings.getInstance(myProject).getActiveCheckers());
        mySavedActiveCheckers = new HashSet<String>(myActiveCheckers);
    }

    @Override
    public void disposeUIResources() {
        // nothing to do here
    }

    private void createUIComponents() {
        myAvailableCheckersTable = new JBTable(new CheckersTableModel());
        myAvailableCheckersTable.getColumnModel().getColumn(0).setMaxWidth(60);
    }

    private class CheckersTableModel extends AbstractTableModel {

        private final String[] myColumnNames = {"Enabled/Disabled", "Checker name"};

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
            return col == 0
                   ? CheckerFrameworkConfigurable.this.myActiveCheckers.contains(clazz.getCanonicalName())
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
            final Class<? extends AbstractTypeProcessor> clazz = CheckerFrameworkSettings.BUILTIN_CHECKERS.get(row);
            if (Boolean.valueOf(String.valueOf(value))) {
                CheckerFrameworkConfigurable.this.myActiveCheckers.add(clazz.getCanonicalName());
            } else {
                CheckerFrameworkConfigurable.this.myActiveCheckers.remove(clazz.getCanonicalName());
            }
            fireTableCellUpdated(row, col);
        }
    }
}


