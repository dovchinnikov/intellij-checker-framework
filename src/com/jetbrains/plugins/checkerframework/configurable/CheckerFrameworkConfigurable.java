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
import java.util.ArrayList;
import java.util.List;

/**
 * @author Daniil Ovchinnikov.
 * @since 7/8/14.
 */
public class CheckerFrameworkConfigurable implements Configurable {

    private JPanel myRootPane;
    private JBTable myAvailableCheckersTable;

    private final Project myProject;
    private final List<Class<? extends AbstractTypeProcessor>> myAvailableCheckers;
    private final List<Class<? extends AbstractTypeProcessor>> myActiveCheckers;
    private final List<Class<? extends AbstractTypeProcessor>> mySavedActiveCheckers;

    public CheckerFrameworkConfigurable(@NotNull Project project) {
        this.myProject = project;
        this.myAvailableCheckers = ClassScanner.findChildren(AbstractTypeProcessor.class, "org.checkerframework.checker");
        this.myActiveCheckers = new ArrayList<Class<? extends AbstractTypeProcessor>>();
        this.mySavedActiveCheckers = new ArrayList<Class<? extends AbstractTypeProcessor>>();
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
        final List<String> activeCheckersStrings = new ArrayList<String>();
        for (final Class clazz : myActiveCheckers) {
            activeCheckersStrings.add(clazz.getCanonicalName());
        }
        CheckerFrameworkSettings.getInstance(myProject).setActiveCheckers(activeCheckersStrings);
        mySavedActiveCheckers.clear();
        mySavedActiveCheckers.addAll(myActiveCheckers);
    }

    @Override
    public void reset() {
        myActiveCheckers.clear();
        for (final String className : CheckerFrameworkSettings.getInstance(myProject).getActiveCheckers()) {
            try {
                myActiveCheckers.add(Class.forName(className).asSubclass(AbstractTypeProcessor.class));
            } catch (ClassNotFoundException ignored) {
            }
        }
        mySavedActiveCheckers.clear();
        mySavedActiveCheckers.addAll(myActiveCheckers);
    }

    @Override
    public void disposeUIResources() {
        // nothing to do here
    }

    private void createUIComponents() {
        myAvailableCheckersTable = new JBTable(new CheckersTableModel());
        myAvailableCheckersTable.getColumnModel().getColumn(0).setMaxWidth(60);
        myAvailableCheckersTable.setRowSelectionAllowed(false);
        myAvailableCheckersTable.setStriped(true);
    }

    private class CheckersTableModel extends AbstractTableModel {

        private final String[] myColumnNames = {"Enabled/Disabled", "Checker name"};

        @Override
        public int getColumnCount() {
            return myColumnNames.length;
        }

        @Override
        public int getRowCount() {
            return CheckerFrameworkConfigurable.this.myAvailableCheckers.size();
        }

        @Override
        public String getColumnName(int col) {
            return myColumnNames[col];
        }

        @Override
        public Object getValueAt(int row, int col) {
            final Class clazz = CheckerFrameworkConfigurable.this.myAvailableCheckers.get(row);
            if (col == 0) {
                return CheckerFrameworkConfigurable.this.myActiveCheckers.contains(clazz);
            } else {
                return clazz.getSimpleName();
            }
        }

        @Override
        public Class getColumnClass(int c) {
            switch (c) {
                case 0:
                    return Boolean.class;
                default:
                    return String.class;
            }
        }

        @Override
        public boolean isCellEditable(int row, int col) {
            return col == 0;
        }

        @Override
        public void setValueAt(Object value, int row, int col) {
            final Class<? extends AbstractTypeProcessor> clazz = CheckerFrameworkConfigurable.this.myAvailableCheckers.get(row);
            if (Boolean.valueOf(String.valueOf(value))) {
                CheckerFrameworkConfigurable.this.myActiveCheckers.add(clazz);
            } else {
                CheckerFrameworkConfigurable.this.myActiveCheckers.remove(clazz);
            }
            fireTableCellUpdated(row, col);
        }
    }
}


