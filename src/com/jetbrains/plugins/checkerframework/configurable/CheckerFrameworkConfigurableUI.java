package com.jetbrains.plugins.checkerframework.configurable;

import com.intellij.openapi.ui.ComboBox;
import com.intellij.ui.table.JBTable;
import org.jetbrains.jps.model.java.compiler.ProcessorConfigProfile;

import javax.swing.*;
import javax.swing.table.TableModel;

/**
 * @author Daniil Ovchinnikov.
 * @since 7/8/14.
 */
public abstract class CheckerFrameworkConfigurableUI {

    private JPanel myRootPane;
    private JBTable myAvailableCheckersTable;
    private ComboBox myProcessorProfilesComboBox;
    private JButton myEnableAllCheckersButton;
    private JButton myDisableAllCheckersButton;

    public JComponent getRoot() {
        return myRootPane;
    }

    protected abstract TableModel getCheckersModel();

    protected abstract ComboBoxModel getProfilesModel();

    private void createUIComponents() {
        myAvailableCheckersTable = new JBTable(getCheckersModel());
        myAvailableCheckersTable.getColumnModel().getColumn(0).setMaxWidth(120);
        myAvailableCheckersTable.getColumnModel().getColumn(0).setPreferredWidth(120);
        myAvailableCheckersTable.getTableHeader().setResizingAllowed(false);
        myAvailableCheckersTable.getTableHeader().setReorderingAllowed(false);

        myProcessorProfilesComboBox = new ComboBox(getProfilesModel());
        myProcessorProfilesComboBox.setSelectedIndex(0);
    }

    public ProcessorConfigProfile getCurrentSelectedProfile() {
        return (ProcessorConfigProfile)myProcessorProfilesComboBox.getSelectedObjects()[0];
    }
}


