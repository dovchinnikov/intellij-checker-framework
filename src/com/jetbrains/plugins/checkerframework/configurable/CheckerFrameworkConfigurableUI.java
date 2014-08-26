package com.jetbrains.plugins.checkerframework.configurable;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.ui.AnActionButton;
import com.intellij.ui.ToolbarDecorator;
import com.intellij.ui.table.JBTable;
import com.jetbrains.plugins.checkerframework.service.CheckerFrameworkSettings;
import org.checkerframework.framework.source.SourceChecker;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;
import java.util.Collection;

public class CheckerFrameworkConfigurableUI {

//    private static final FileChooserDescriptor JAR_DESCRIPTOR = new FileChooserDescriptor(false, false, true, true, false, false);

    private       JPanel                            myRootPane;
    //    private TextFieldWithBrowseButton myPathToCheckerJarField;
//    private HyperlinkLabel            myDownloadCheckerLink;
    private       JPanel                            myCheckersPanel;
    private       JPanel                            myOptionsPanel;
    //    private JBLabel                           myInfoLabel;
    private final CheckersTableModel<SourceChecker> myCheckersTableModel;
    private final OptionsTableModel                 myOptionsTableModel;


    public CheckerFrameworkConfigurableUI(@NotNull final CheckerFrameworkSettings settings) {

//        myPathToCheckerJarField.getTextField().getDocument().addDocumentListener(new PathToJarChangeListener());
//        myPathToCheckerJarField.addBrowseFolderListener(
//            null,
//            new ComponentWithBrowseButton.BrowseFolderActionListener<JTextField>(
//                null,
//                "Select path to checker.jar",
//                myPathToCheckerJarField,
//                null,
//                JAR_DESCRIPTOR,
//                TextComponentAccessor.TEXT_FIELD_WHOLE_TEXT
//            )
//        );
//
//        myDownloadCheckerLink.setHyperlinkText("Click here to download Checker Framework");
//        myDownloadCheckerLink.addHyperlinkListener(new DownloadCheckerFrameworkLinkListener(myRootPane, myPathToCheckerJarField));

        myCheckersTableModel = new CheckersTableModel<SourceChecker>(settings.getBuiltInCheckers(), settings.getEnabledCheckerClasses());
        final JBTable myCheckersTable = new JBTable(myCheckersTableModel);
        myCheckersTable.setAutoCreateRowSorter(true);
        myCheckersTable.setAutoCreateColumnsFromModel(true);
        myCheckersTable.setStriped(true);
        myCheckersTable.setRowSelectionAllowed(false);
        myCheckersTable.getColumnModel().getColumn(0).setMaxWidth(120);
        myCheckersTable.getTableHeader().setReorderingAllowed(false);
        myCheckersTable.getRowSorter().toggleSortOrder(1);
        myCheckersPanel.add(
            ToolbarDecorator.createDecorator(
                myCheckersTable
                //).addExtraAction(
                //    new AddCustomCheckerButton()
            ).addExtraAction(
                new AnActionButton("Select all", AllIcons.Actions.Selectall) {
                    @Override
                    public void actionPerformed(AnActionEvent e) {
                        for (int i = 0; i < myCheckersTable.getRowCount(); i++) {
                            myCheckersTable.setValueAt(true, i, 0);
                        }
                    }
                }
            ).addExtraAction(
                new AnActionButton("Unselect all", AllIcons.Actions.Unselectall) {
                    @Override
                    public void actionPerformed(AnActionEvent e) {
                        for (int i = 0; i < myCheckersTable.getRowCount(); i++) {
                            myCheckersTable.setValueAt(false, i, 0);
                        }
                    }
                }
                //).setRemoveAction(
                //    new AnActionButtonRunnable() {
                //        @Override
                //        public void run(AnActionButton button) {
                //            int selectedRow = myCheckersTable.getSelectedRow();
                //            int selectedCheckerIndex = myCheckersTable.convertRowIndexToModel(selectedRow);
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
        myOptionsTableModel = new OptionsTableModel(settings.getOptions());
        final JBTable myOptionsTable = new JBTable(myOptionsTableModel);
        myOptionsPanel.add(
            ToolbarDecorator.createDecorator(myOptionsTable)
//                .setAddAction(
//                    new AnActionButtonRunnable() {
//                        @Override
//                        public void run(AnActionButton anActionButton) {
//                            final TableCellEditor cellEditor = myOptionsTable.getCellEditor();
//                            if (cellEditor != null) {
//                                cellEditor.stopCellEditing();
//                            }
//                            final TableModel model = myOptionsTable.getModel();
//                            ((EditableModel) model).addRow();
//                            TableUtil.editCellAt(myOptionsTable, model.getRowCount() - 1, 0);
//                        }
//                    }
//                )
                .createPanel(),
            BorderLayout.CENTER
        );

//        myInfoLabel.setIcon(AllIcons.General.BalloonWarning);
    }

    public JComponent getRoot() {
        return myRootPane;
    }

    public Collection<Class<? extends SourceChecker>> getConfiguredEnabledCheckers() {
        return myCheckersTableModel.getEnabledClasses();
    }

    public Collection<String> getConfiguredOptions() {
        return myOptionsTableModel.getOptions();
    }

    public void reset(CheckerFrameworkSettings settings) {
        myCheckersTableModel.setEnabledClasses(settings.getEnabledCheckerClasses());
        myOptionsTableModel.setOptions(settings.getOptions());
    }

//    private class PathToJarChangeListener extends DocumentAdapter {
//
//        @Override
//        protected void textChanged(DocumentEvent e) {
//            mySettings.setPathToCheckerJar(myPathToCheckerJarField.getText());
//            myInfoLabel.setText(mySettings.valid() ? mySettings.getVersion() : mySettings.getErrorMessage());
//            myInfoLabel.setIcon(mySettings.valid() ? AllIcons.General.BalloonInformation : AllIcons.General.BalloonWarning);
//            myCheckersTable.setEnabled(mySettings.valid());
//            myCheckersTableModel.fireTableDataChanged();
//        }
//    }

//    @SuppressWarnings("UnusedDeclaration")
//    private class AddCustomCheckerButton extends AnActionButton {
//
//        private final @Nullable PsiClass myProcessorInterface;
//        private final ClassFilter myClassFilter = new ClassFilter() {
//            @Override
//            public boolean isAccepted(@NotNull final PsiClass psiClazz) {
//                assert myProcessorInterface != null;
//                return psiClazz.getQualifiedName() != null
//                    && !mySettings.getBuiltInCheckers().contains(psiClazz.getQualifiedName())
//                    && psiClazz.isInheritor(myProcessorInterface, true);
//            }
//        };
//
//        public AddCustomCheckerButton() {
//            super("Add custom checker", AllIcons.ToolbarDecorator.AddClass);
//            myProcessorInterface = JavaPsiFacade.getInstance(myProject).findClass(
//                Stuff.CHECKERS_BASE_CLASS_FQN,
//                GlobalSearchScope.allScope(myProject)
//            );
//            setEnabled(myProcessorInterface != null);
//        }
//
//        @Override
//        public void actionPerformed(AnActionEvent e) {
//            final TreeClassChooser myChooser = TreeClassChooserFactory.getInstance(myProject).createNoInnerClassesScopeChooser(
//                UIBundle.message("class.filter.editor.choose.class.title"),
//                GlobalSearchScope.allScope(myProject),
//                myClassFilter,
//                null
//            );
//            myChooser.showDialog();
//            final PsiClass selectedClass = myChooser.getSelected();
//            if (selectedClass != null) {
//                final String fqn = selectedClass.getQualifiedName();
//                assert fqn != null;
//                mySettings.addCustomChecker(fqn);
//                myCheckersTableModel.fireTableDataChanged();
//            }
//        }
//    }
}
