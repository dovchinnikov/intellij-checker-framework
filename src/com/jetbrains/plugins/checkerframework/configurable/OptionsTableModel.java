package com.jetbrains.plugins.checkerframework.configurable;

import com.intellij.util.ui.EditableModel;
import org.jetbrains.annotations.NotNull;

import javax.swing.table.AbstractTableModel;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class OptionsTableModel extends AbstractTableModel implements EditableModel {

    private final List<String> myOptions;

    public OptionsTableModel(final Collection<String> options) {
        this.myOptions = new ArrayList<String>(options);
    }

    public List<String> getOptions() {
        return myOptions;
    }

    public void setOptions(Collection<String> options) {
        myOptions.clear();
        myOptions.addAll(options);
    }

    @NotNull
    @Override
    public String getColumnName(int column) {
        return "Option";
    }

    @NotNull
    @Override
    public Class<String> getColumnClass(int columnIndex) {
        return String.class;
    }

    @Override
    public int getRowCount() {
        return myOptions.size();
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
        return myOptions.get(rowIndex);
    }

    @Override
    public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
        assert columnIndex == 0;
        myOptions.set(rowIndex, String.valueOf(aValue));
    }

    @Override
    public void addRow() {
        myOptions.add("");
        final int index = myOptions.size() - 1;
        fireTableRowsInserted(index, index);
    }

    @Override
    public void removeRow(int idx) {
        myOptions.remove(idx);
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
