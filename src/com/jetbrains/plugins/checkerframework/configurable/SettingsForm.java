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
public class SettingsForm implements Configurable {

    private JPanel rootPane;
    private JBTable availableCheckersTable;

    private final Project project;
    private final List<Class<? extends AbstractTypeProcessor>> availableCheckers;
    private final List<Class<? extends AbstractTypeProcessor>> activeCheckers;
    private final List<Class<? extends AbstractTypeProcessor>> savedActiveCheckers;

    public SettingsForm(@NotNull Project project) throws ClassNotFoundException {
        this.project = project;
        this.availableCheckers = ClassScanner.findChildren(AbstractTypeProcessor.class);
        this.activeCheckers = new ArrayList<Class<? extends AbstractTypeProcessor>>();
        this.savedActiveCheckers = new ArrayList<Class<? extends AbstractTypeProcessor>>();
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
        return rootPane;
    }

    @Override
    public boolean isModified() {
        return !(
            savedActiveCheckers.size() == activeCheckers.size()
            && savedActiveCheckers.containsAll(activeCheckers)
            && activeCheckers.containsAll(savedActiveCheckers)
        );
    }

    @Override
    public void apply() throws ConfigurationException {
        final List<String> activeCheckersStrings = new ArrayList<String>();
        for (final Class clazz : activeCheckers) {
            activeCheckersStrings.add(clazz.getCanonicalName());
        }
        Settings.getInstance(project).setActiveCheckers(activeCheckersStrings);
        savedActiveCheckers.clear();
        savedActiveCheckers.addAll(activeCheckers);
    }

    @Override
    public void reset() {
        activeCheckers.clear();
        for (final String className : Settings.getInstance(project).getActiveCheckers()) {
            try {
                activeCheckers.add(Class.forName(className).asSubclass(AbstractTypeProcessor.class));
            } catch (ClassNotFoundException ignored) {
            }
        }
        savedActiveCheckers.clear();
        savedActiveCheckers.addAll(activeCheckers);
    }

    @Override
    public void disposeUIResources() {
        // nothing to do here
    }

    private void createUIComponents() {
        availableCheckersTable = new JBTable(new CheckersTableModel());
        availableCheckersTable.getColumnModel().getColumn(0).setMaxWidth(60);
        availableCheckersTable.setRowSelectionAllowed(false);
        availableCheckersTable.setStriped(true);
    }

    class CheckersTableModel extends AbstractTableModel {

        private final String[] columnNames = {"Enabled/Disabled", "Checker name"};

        @Override
        public int getColumnCount() {
            return columnNames.length;
        }

        @Override
        public int getRowCount() {
            return SettingsForm.this.availableCheckers.size();
        }

        @Override
        public String getColumnName(int col) {
            return columnNames[col];
        }

        @Override
        public Object getValueAt(int row, int col) {
            final Class clazz = SettingsForm.this.availableCheckers.get(row);
            if (col == 0) {
                return SettingsForm.this.activeCheckers.contains(clazz);
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
            final Class<? extends AbstractTypeProcessor> clazz = SettingsForm.this.availableCheckers.get(row);
            if ((Boolean)value) {
                SettingsForm.this.activeCheckers.add(clazz);
            } else {
                SettingsForm.this.activeCheckers.remove(clazz);
            }
            fireTableCellUpdated(row, col);
        }
    }
}


