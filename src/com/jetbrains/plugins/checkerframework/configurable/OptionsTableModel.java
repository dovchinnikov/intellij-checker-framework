package com.jetbrains.plugins.checkerframework.configurable;

import com.intellij.util.ui.EditableModel;

import javax.swing.table.AbstractTableModel;
import java.util.List;

public class OptionsTableModel extends AbstractTableModel implements EditableModel {

    private List<String> myData;

    public OptionsTableModel(List<String> data) {
        myData = data;
    }

    public void setData(List<String> data) {
        myData = data;
        fireTableDataChanged();
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
        return myData.size();
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
        return myData.get(rowIndex);
    }

    @Override
    public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
        myData.set(rowIndex, String.valueOf(aValue));
    }

    @Override
    public void addRow() {
        myData.add(null);
        final int index = myData.size() - 1;
        fireTableRowsInserted(index, index);
    }

    @Override
    public void removeRow(int idx) {
        myData.remove(idx);
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
