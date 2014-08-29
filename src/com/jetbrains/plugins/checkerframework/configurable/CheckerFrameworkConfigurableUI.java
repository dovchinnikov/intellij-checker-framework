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

    private       JPanel                            myRootPane;
    private       JPanel                            myCheckersPanel;
    private       JPanel                            myOptionsPanel;
    private final CheckersTableModel<SourceChecker> myCheckersTableModel;
    private final OptionsTableModel                 myOptionsTableModel;

    public CheckerFrameworkConfigurableUI(@NotNull final CheckerFrameworkSettings settings) {
        myCheckersTableModel = new CheckersTableModel<SourceChecker>(settings.getBuiltInCheckers(), settings.getEnabledCheckerClasses());
        myOptionsTableModel = new OptionsTableModel(settings.getOptions());

        final JBTable myCheckersTable = new JBTable(myCheckersTableModel);
        myCheckersTable.setAutoCreateRowSorter(true);
        myCheckersTable.setAutoCreateColumnsFromModel(true);
        myCheckersTable.setStriped(true);
        myCheckersTable.setRowSelectionAllowed(false);
        myCheckersTable.getColumnModel().getColumn(0).setMaxWidth(120);
        myCheckersTable.getTableHeader().setReorderingAllowed(false);
        myCheckersTable.getRowSorter().toggleSortOrder(1);
        final JBTable myOptionsTable = new JBTable(myOptionsTableModel);

        myCheckersPanel.add(
            ToolbarDecorator.createDecorator(
                myCheckersTable
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
            ).createPanel(),
            BorderLayout.CENTER
        );
        myOptionsPanel.add(
            ToolbarDecorator.createDecorator(myOptionsTable).createPanel(),
            BorderLayout.CENTER
        );
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
}
