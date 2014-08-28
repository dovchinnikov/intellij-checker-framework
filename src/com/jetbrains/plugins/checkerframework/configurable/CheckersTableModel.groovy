package com.jetbrains.plugins.checkerframework.configurable

import groovy.transform.CompileStatic

import javax.swing.table.AbstractTableModel

import static java.lang.Boolean.TRUE

@CompileStatic
public class CheckersTableModel<C> extends AbstractTableModel {

    private static final String[] COLUMN_NAMES = ["Enabled", "Checker class"];
    private static final Class[] COLUMN_CLASSES = [Boolean.class, String.class];

    private final List<Entry<C>> myEntries;

    public CheckersTableModel(Collection<Class<? extends C>> all, Collection<Class<? extends C>> enabled) {
        myEntries = all.collect {
            Class<? extends C> clazz ->
                new Entry<C>(
                    enabled.contains(clazz),
                    clazz
                )
        }.asList()
    }

    public Collection<Class<? extends C>> getEnabledClasses() {
        myEntries.findAll {
            Entry entry -> entry.enabled
        }.collect {
            Entry entry -> entry.clazz
        }
    }

    public void setEnabledClasses(Collection<Class<? extends C>> classes) {
        myEntries.every {Entry entry -> entry.enabled = classes.contains(entry.clazz)}
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
        return myEntries.size();
    }

    @Override
    public boolean isCellEditable(int row, int col) {
        return col == 0;
    }

    @Override
    public Object getValueAt(int row, int col) {
        final Entry entry = myEntries.get(row);
        return col == 0 ? entry.enabled : entry.clazz.getCanonicalName();
    }

    @Override
    public void setValueAt(Object value, int row, int col) {
        assert col == 0;
        myEntries.get(row).enabled = TRUE.equals(value);
        fireTableCellUpdated(row, col);
    }

    private static class Entry<C> {
        boolean enabled;
        Class<? extends C> clazz;

        public Entry(boolean enabled, Class<? extends C> clazz) {
            this.enabled = enabled;
            this.clazz = clazz;
        }
    }
}
