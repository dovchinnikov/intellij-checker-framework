package com.jetbrains.plugins.checkerframework.configurable;

import com.intellij.icons.AllIcons;
import com.intellij.ide.util.ClassFilter;
import com.intellij.ide.util.TreeClassChooser;
import com.intellij.ide.util.TreeClassChooserFactory;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.fileChooser.FileChooserDescriptor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.ComponentWithBrowseButton;
import com.intellij.openapi.ui.TextComponentAccessor;
import com.intellij.openapi.ui.TextFieldWithBrowseButton;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiClass;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.ui.*;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.table.JBTable;
import com.intellij.util.ui.EditableModel;
import com.jetbrains.plugins.checkerframework.service.CheckerFrameworkSettings;
import com.jetbrains.plugins.checkerframework.service.Stuff;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableModel;
import java.awt.*;

public class CheckerFrameworkConfigurableUI {

    private static final FileChooserDescriptor JAR_DESCRIPTOR = new FileChooserDescriptor(false, false, true, true, false, false);

    private final Project                  myProject;
    private final CheckerFrameworkSettings mySettings;
    private final CheckersTableModel       myCheckersTableModel;

    private JPanel                    myRootPane;
    private TextFieldWithBrowseButton myPathToCheckerJarField;
    private HyperlinkLabel            myDownloadCheckerLink;
    private JPanel                    myAvailableCheckersPanel;
    private JBTable                   myAvailableCheckersTable;
    private JPanel                    myOptionsPanel;
    private JBTable                   myOptionsTable;
    private JBLabel                   myInfoLabel;

    public CheckerFrameworkConfigurableUI(final Project project, final CheckerFrameworkSettings settings) {
        myProject = project;
        mySettings = settings;

        myPathToCheckerJarField.getTextField().getDocument().addDocumentListener(new PathToJarChangeListener());
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

        myCheckersTableModel = new CheckersTableModel(settings);
        myAvailableCheckersTable = new JBTable(myCheckersTableModel);
        myAvailableCheckersTable.setAutoCreateRowSorter(true);
        myAvailableCheckersTable.setAutoCreateColumnsFromModel(true);
        myAvailableCheckersTable.setStriped(true);
        myAvailableCheckersTable.setRowSelectionAllowed(false);
        myAvailableCheckersTable.getColumnModel().getColumn(0).setMaxWidth(120);
        myAvailableCheckersTable.getTableHeader().setReorderingAllowed(false);
        myAvailableCheckersTable.getRowSorter().toggleSortOrder(1);
        myAvailableCheckersPanel.add(
            ToolbarDecorator.createDecorator(
                myAvailableCheckersTable
                //).addExtraAction(
                //    new AddCustomCheckerButton()
            ).addExtraAction(
                new AnActionButton("Select all", AllIcons.Actions.Selectall) {
                    @Override
                    public void actionPerformed(AnActionEvent e) {
                        for (int i = 0; i < myAvailableCheckersTable.getRowCount(); i++) {
                            myAvailableCheckersTable.setValueAt(true, i, 0);
                        }
                    }
                }
            ).addExtraAction(
                new AnActionButton("Unselect all", AllIcons.Actions.Unselectall) {
                    @Override
                    public void actionPerformed(AnActionEvent e) {
                        for (int i = 0; i < myAvailableCheckersTable.getRowCount(); i++) {
                            myAvailableCheckersTable.setValueAt(false, i, 0);
                        }
                    }
                }
                //).setRemoveAction(
                //    new AnActionButtonRunnable() {
                //        @Override
                //        public void run(AnActionButton button) {
                //            int selectedRow = myAvailableCheckersTable.getSelectedRow();
                //            int selectedCheckerIndex = myAvailableCheckersTable.convertRowIndexToModel(selectedRow);
                //            String selectedChecker = String.valueOf(myCheckersTableModel.getValueAt(selectedCheckerIndex, 1));
                //            System.out.println(selectedChecker);
                //        }
                //    }
                //).setRemoveActionUpdater(
                //    new AnActionButtonUpdater() {
                //        @Override
                //        public boolean isEnabled(AnActionEvent e) {
                //            return true;
                //        }
                //    }
            ).createPanel(),
            BorderLayout.CENTER
        );

        myOptionsTable = new JBTable(new OptionsTableModel(settings));
        myOptionsPanel.add(
            ToolbarDecorator.createDecorator(myOptionsTable)
                .setAddAction(
                    new AnActionButtonRunnable() {
                        @Override
                        public void run(AnActionButton anActionButton) {
                            final TableCellEditor cellEditor = myOptionsTable.getCellEditor();
                            if (cellEditor != null) {
                                cellEditor.stopCellEditing();
                            }
                            final TableModel model = myOptionsTable.getModel();
                            ((EditableModel) model).addRow();
                            TableUtil.editCellAt(myOptionsTable, model.getRowCount() - 1, 0);
                        }
                    }
                ).createPanel(),
            BorderLayout.CENTER
        );

        myInfoLabel.setIcon(AllIcons.General.BalloonWarning);
    }

    public JComponent getRoot() {
        return myRootPane;
    }

    public void reset() {
        myPathToCheckerJarField.setText(mySettings.getPathToCheckerJar());
    }

    private class PathToJarChangeListener extends DocumentAdapter {

        @Override
        protected void textChanged(DocumentEvent e) {
            mySettings.setPathToCheckerJar(myPathToCheckerJarField.getText());
            myInfoLabel.setText(mySettings.valid() ? mySettings.getVersion() : mySettings.getErrorMessage());
            myInfoLabel.setIcon(mySettings.valid() ? AllIcons.General.BalloonInformation : AllIcons.General.BalloonWarning);
            myAvailableCheckersTable.setEnabled(mySettings.valid());
            myCheckersTableModel.fireTableDataChanged();
        }
    }

    @SuppressWarnings("UnusedDeclaration")
    private class AddCustomCheckerButton extends AnActionButton {

        private final @Nullable PsiClass myProcessorInterface;
        private final ClassFilter myClassFilter = new ClassFilter() {
            @Override
            public boolean isAccepted(final PsiClass psiClazz) {
                assert myProcessorInterface != null;
                return psiClazz.getQualifiedName() != null
                    && !mySettings.getBuiltInCheckers().contains(psiClazz.getQualifiedName())
                    && psiClazz.isInheritor(myProcessorInterface, true);
            }
        };

        public AddCustomCheckerButton() {
            super("Add custom checker", AllIcons.ToolbarDecorator.AddClass);
            myProcessorInterface = JavaPsiFacade.getInstance(myProject).findClass(
                Stuff.CHECKERS_BASE_CLASS_FQN,
                GlobalSearchScope.allScope(myProject)
            );
            setEnabled(myProcessorInterface != null);
        }

        @Override
        public void actionPerformed(AnActionEvent e) {
            final TreeClassChooser myChooser = TreeClassChooserFactory.getInstance(myProject).createNoInnerClassesScopeChooser(
                UIBundle.message("class.filter.editor.choose.class.title"),
                GlobalSearchScope.allScope(myProject),
                myClassFilter,
                null
            );
            myChooser.showDialog();
            final PsiClass selectedClass = myChooser.getSelected();
            if (selectedClass != null) {
                final String fqn = selectedClass.getQualifiedName();
                assert fqn != null;
                mySettings.addCustomChecker(fqn);
                myCheckersTableModel.fireTableDataChanged();
            }
        }
    }
}
