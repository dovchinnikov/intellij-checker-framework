package com.jetbrains.plugins.checkerframework.configurable;

import com.intellij.openapi.ui.TextFieldWithBrowseButton;
import com.intellij.ui.table.JBTable;

import javax.swing.*;
import javax.swing.table.TableModel;
import java.awt.event.ActionEvent;

public abstract class CheckerFrameworkConfigurableUI {

    private JPanel myRootPane;
    private JBTable myAvailableCheckersTable;
    private JButton myEnableAllCheckersButton;
    private JButton myDisableAllCheckersButton;
    private TextFieldWithBrowseButton myPathToCheckerJarField;

    public CheckerFrameworkConfigurableUI() {
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
    }

    public JComponent getRoot() {
        return myRootPane;
    }

    public String getPathToCheckerJar() {
        return myPathToCheckerJarField.getText();
    }

    public void setPathToCheckerJar(String pathToCheckerJar) {
        myPathToCheckerJarField.setText(pathToCheckerJar);
    }

    protected abstract TableModel getCheckersModel();

    private void createUIComponents() {
        myAvailableCheckersTable = new JBTable(getCheckersModel());
        myAvailableCheckersTable.getColumnModel().getColumn(0).setMaxWidth(120);
        myAvailableCheckersTable.getColumnModel().getColumn(0).setPreferredWidth(120);
        myAvailableCheckersTable.getTableHeader().setResizingAllowed(false);
        myAvailableCheckersTable.getTableHeader().setReorderingAllowed(false);
    }
}


