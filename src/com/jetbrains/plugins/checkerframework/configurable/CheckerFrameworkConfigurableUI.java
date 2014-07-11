package com.jetbrains.plugins.checkerframework.configurable;

import com.intellij.openapi.ui.ComboBox;
import com.intellij.ui.table.JBTable;
import org.jetbrains.jps.model.java.compiler.ProcessorConfigProfile;

import javax.swing.*;
import javax.swing.table.TableModel;
import java.awt.event.ActionEvent;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;

public abstract class CheckerFrameworkConfigurableUI {

    private JPanel myRootPane;
    private JBTable myAvailableCheckersTable;
    private ComboBox myProcessorProfilesComboBox;
    private JButton myEnableAllCheckersButton;
    private JButton myDisableAllCheckersButton;
    private JLabel myProfileStateLabel;
    private JButton myEnableProfileButton;

    public CheckerFrameworkConfigurableUI() {
        myProcessorProfilesComboBox.addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent e) {
                if (e.getStateChange() == ItemEvent.SELECTED) {
                    ProcessorConfigProfile configProfile = getCurrentSelectedProfile();
                    myProfileStateLabel.setText("This profile is " + (configProfile.isEnabled() ? "enabled" : "disabled"));
                    myEnableProfileButton.setVisible(!configProfile.isEnabled());
                }
            }
        });
        myEnableAllCheckersButton.addActionListener(new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                for (int i = 0; i < myAvailableCheckersTable.getRowCount(); i++) {
                    myAvailableCheckersTable.setValueAt(true, i, 0);
                }
            }
        });
        myDisableAllCheckersButton.addActionListener(new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                for (int i = 0; i < myAvailableCheckersTable.getRowCount(); i++) {
                    myAvailableCheckersTable.setValueAt(false, i, 0);
                }
            }
        });
        myEnableProfileButton.addActionListener(new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                getCurrentSelectedProfile().setEnabled(true);
                myProfileStateLabel.setText("This profile is enabled");
                myEnableProfileButton.setVisible(false);
            }
        });
        myProcessorProfilesComboBox.setSelectedIndex(0);
    }

    public JComponent getRoot() {
        return myRootPane;
    }

    public ProcessorConfigProfile getCurrentSelectedProfile() {
        return (ProcessorConfigProfile)myProcessorProfilesComboBox.getSelectedItem();
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
    }
}


