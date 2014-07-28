package com.jetbrains.plugins.checkerframework.configurable;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.fileChooser.FileChooserDescriptor;
import com.intellij.openapi.ui.ComponentWithBrowseButton;
import com.intellij.openapi.ui.TextComponentAccessor;
import com.intellij.openapi.ui.TextFieldWithBrowseButton;
import com.intellij.ui.*;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.table.JBTable;
import com.intellij.util.ui.EditableModel;

import javax.swing.*;
import javax.swing.event.DocumentListener;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableModel;

public abstract class CheckerFrameworkConfigurableUI {

    private static final FileChooserDescriptor JAR_DESCRIPTOR = new FileChooserDescriptor(false, false, true, true, false, false);

    private JPanel myRootPane;
    @SuppressWarnings("UnusedDeclaration")
    private JPanel myAvailableCheckersPanel;
    private JBTable myAvailableCheckersTable;
    private TextFieldWithBrowseButton myPathToCheckerJarField;
    private HyperlinkLabel myDownloadCheckerLink;
    private JBLabel myErrorLabel;
    @SuppressWarnings("UnusedDeclaration")
    private JPanel myOptionsPanel;
    private JBTable myOptionsTable;

    public CheckerFrameworkConfigurableUI() {
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
        myErrorLabel.setIcon(AllIcons.General.BalloonWarning);
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

    protected abstract DocumentListener getPathToJarChangeListener();

    protected abstract TableModel getCheckersModel();

    protected abstract TableModel getOptionsModel();

    private void createUIComponents() {
        myPathToCheckerJarField = new TextFieldWithBrowseButton();
        myPathToCheckerJarField.getTextField().getDocument().addDocumentListener(getPathToJarChangeListener());

        myAvailableCheckersTable = new JBTable(getCheckersModel());
        myAvailableCheckersTable.setAutoCreateRowSorter(true);
        myAvailableCheckersTable.setAutoCreateColumnsFromModel(true);
        myAvailableCheckersTable.setStriped(true);
        myAvailableCheckersTable.setRowSelectionAllowed(false);
        myAvailableCheckersTable.getColumnModel().getColumn(0).setMaxWidth(120);
        myAvailableCheckersTable.getTableHeader().setReorderingAllowed(false);
        myAvailableCheckersTable.getRowSorter().toggleSortOrder(1);

        myAvailableCheckersPanel = ToolbarDecorator.createDecorator(myAvailableCheckersTable)
            .addExtraAction(new AnActionButton("Select all", AllIcons.Actions.Selectall) {
                @Override
                public void actionPerformed(AnActionEvent e) {
                    for (int i = 0; i < myAvailableCheckersTable.getRowCount(); i++) {
                        myAvailableCheckersTable.setValueAt(true, i, 0);
                    }
                }
            }).addExtraAction(new AnActionButton("Unselect all", AllIcons.Actions.Unselectall) {
                @Override
                public void actionPerformed(AnActionEvent e) {
                    for (int i = 0; i < myAvailableCheckersTable.getRowCount(); i++) {
                        myAvailableCheckersTable.setValueAt(false, i, 0);
                    }
                }
            }).createPanel();

        myOptionsTable = new JBTable(getOptionsModel());
        myOptionsPanel = ToolbarDecorator.createDecorator(myOptionsTable)
            .setAddAction(new AnActionButtonRunnable() {
                @Override
                public void run(AnActionButton anActionButton) {
                    final TableCellEditor cellEditor = myOptionsTable.getCellEditor();
                    if (cellEditor != null) {
                        cellEditor.stopCellEditing();
                    }
                    final TableModel model = myOptionsTable.getModel();
                    ((EditableModel)model).addRow();
                    TableUtil.editCellAt(myOptionsTable, model.getRowCount() - 1, 0);
                }
            }).createPanel();
    }
}
