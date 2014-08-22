package com.jetbrains.plugins.checkerframework.configurable;

import com.jetbrains.plugins.checkerframework.service.CheckerFrameworkSettings;

import javax.swing.table.AbstractTableModel;

public class CheckersTableModel extends AbstractTableModel {

    private static final String[] COLUMN_NAMES   = {"Enabled", "Checker class"};
    private static final Class[]  COLUMN_CLASSES = {Boolean.class, String.class};

    private final CheckerFrameworkSettings mySettings;

    public CheckersTableModel(CheckerFrameworkSettings settings) {
        mySettings = settings;
    }

    @Override
    public int getColumnCount() {
        return COLUMN_NAMES.length;
    }

    @Override
    public String getColumnName(int column) {
        return COLUMN_NAMES[column];
    }

    @Override
    public Class getColumnClass(int column) {
        return COLUMN_CLASSES[column];
    }

    @Override
    public int getRowCount() {
        return mySettings.getCheckers().size();
    }

    @Override
    public boolean isCellEditable(int row, int col) {
        return col == 0;
    }

    @Override
    public Object getValueAt(int row, int col) {
        final String clazzName = mySettings.getCheckers().get(row);
        return col == 0 ? mySettings.getEnabledCheckers().contains(clazzName) : clazzName;
    }

    @Override
    public void setValueAt(Object value, int row, int col) {
        final String clazzName = mySettings.getCheckers().get(row);
        if (Boolean.TRUE.equals(value)) {
            mySettings.getEnabledCheckers().add(clazzName);
        } else {
            mySettings.getEnabledCheckers().remove(clazzName);
        }
        fireTableCellUpdated(row, col);
    }
}
