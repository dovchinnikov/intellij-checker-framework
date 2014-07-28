package com.jetbrains.plugins.checkerframework.configurable;

import com.intellij.openapi.fileChooser.FileChooserDescriptor;
import com.intellij.openapi.ui.ComponentWithBrowseButton;
import com.intellij.openapi.ui.TextComponentAccessor;
import com.intellij.openapi.ui.TextFieldWithBrowseButton;
import com.intellij.ui.HyperlinkLabel;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.table.JBTable;
import com.intellij.util.ui.UIUtil;

import javax.swing.*;
import javax.swing.event.DocumentListener;
import javax.swing.table.TableModel;
import java.awt.event.ActionEvent;

public abstract class CheckerFrameworkConfigurableUI {

    private static final FileChooserDescriptor JAR_DESCRIPTOR = new FileChooserDescriptor(false, false, true, true, false, false);

    private JPanel myRootPane;
    private JBTable myAvailableCheckersTable;
    private JButton myEnableAllCheckersButton;
    private JButton myDisableAllCheckersButton;
    private TextFieldWithBrowseButton myPathToCheckerJarField;
    private HyperlinkLabel myDownloadCheckerLink;
    private JBLabel myErrorLabel;

    public CheckerFrameworkConfigurableUI() {
        myAvailableCheckersTable.getRowSorter().toggleSortOrder(1);
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
        myPathToCheckerJarField.addBrowseFolderListener(
            null,
            new ComponentWithBrowseButton.BrowseFolderActionListener<JTextField>(
                null,
                "Select path to checker.jar",
                myPathToCheckerJarField,
                null,
                JAR_DESCRIPTOR,
                TextComponentAccessor.TEXT_FIELD_WHOLE_TEXT
            )
        );
        myDownloadCheckerLink.setHyperlinkText("Click here to download Checker Framework");
        myDownloadCheckerLink.addHyperlinkListener(new DownloadCheckerFrameworkLinkListener(myRootPane, myPathToCheckerJarField));
        myErrorLabel.setIcon(UIUtil.getBalloonWarningIcon());
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

    public void showWarning(String text) {
        myErrorLabel.setText(text);
        myErrorLabel.setVisible(true);
    }

    public void hideWarning() {
        myErrorLabel.setVisible(false);
    }

    protected abstract TableModel getCheckersModel();

    protected abstract DocumentListener getPathToJarChangeListener();

    private void createUIComponents() {
        myAvailableCheckersTable = new JBTable(getCheckersModel());
        myAvailableCheckersTable.getColumnModel().getColumn(0).setMaxWidth(120);
        myAvailableCheckersTable.getColumnModel().getColumn(0).setPreferredWidth(120);
        myAvailableCheckersTable.getTableHeader().setResizingAllowed(false);
        myAvailableCheckersTable.getTableHeader().setReorderingAllowed(false);

        myPathToCheckerJarField = new TextFieldWithBrowseButton();
        myPathToCheckerJarField.getTextField().getDocument().addDocumentListener(getPathToJarChangeListener());
    }
}
