package com.jetbrains.plugins.checkerframework.configurable;

import com.intellij.util.ui.EditableModel;
import org.jetbrains.annotations.NotNull;

import javax.swing.table.AbstractTableModel;

public class OptionsTableModel extends AbstractTableModel implements EditableModel {

    private final @NotNull CheckerFrameworkSettings mySettings;

    public OptionsTableModel(@NotNull CheckerFrameworkSettings settings) {
        mySettings = settings;
    }

    @Override
    public String getColumnName(int column) {
        return "Option";
    }

    @Override
    public Class<String> getColumnClass(int columnIndex) {
        return String.class;
    }

    @Override
    public int getRowCount() {
        return mySettings.getOptions().size();
    }

    @Override
    public int getColumnCount() {
        return 1;
    }

    @Override
    public boolean isCellEditable(int rowIndex, int columnIndex) {
        return columnIndex == 0;
    }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
        return mySettings.getOptions().get(rowIndex);
    }

    @Override
    public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
        mySettings.getOptions().set(rowIndex, String.valueOf(aValue));
    }

    @Override
    public void addRow() {
        mySettings.getOptions().add("");
        final int index = mySettings.getOptions().size() - 1;
        fireTableRowsInserted(index, index);
    }

    @Override
    public void removeRow(int idx) {
        mySettings.getOptions().remove(idx);
        fireTableRowsDeleted(idx, idx);
    }

    @Override
    public void exchangeRows(int oldIndex, int newIndex) {
    }

    @Override
    public boolean canExchangeRows(int oldIndex, int newIndex) {
        return false;
    }
}
