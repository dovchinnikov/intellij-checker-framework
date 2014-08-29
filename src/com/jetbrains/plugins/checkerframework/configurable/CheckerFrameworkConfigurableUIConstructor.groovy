package com.jetbrains.plugins.checkerframework.configurable

import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.Project
import com.intellij.ui.AnActionButton
import com.intellij.ui.ToolbarDecorator
import com.intellij.ui.table.JBTable
import com.jetbrains.plugins.checkerframework.service.CheckerFrameworkLibrary
import com.jetbrains.plugins.checkerframework.service.Stuff
import groovy.transform.CompileStatic
import org.jetbrains.jps.model.java.compiler.ProcessorConfigProfile

import javax.swing.*

import static com.jetbrains.plugins.checkerframework.service.Stuff.BUILTIN_CHECKERS
import static java.awt.BorderLayout.CENTER

@CompileStatic
abstract class CheckerFrameworkConfigurableUIConstructor extends CheckerFrameworkConfigurableUI {

    CheckerFrameworkConfigurableUIConstructor(final Project project) {
        super(project)
        final JBTable myCheckersTable = new JBTable(myCheckersTableModel);
        myCheckersTable.setAutoCreateRowSorter(true);
        myCheckersTable.setAutoCreateColumnsFromModel(true);
        myCheckersTable.setStriped(true);
        myCheckersTable.setRowSelectionAllowed(false);
        myCheckersTable.getColumnModel().getColumn(0).setMaxWidth(120);
        myCheckersTable.getTableHeader().setReorderingAllowed(false);
        myCheckersTable.getRowSorter().toggleSortOrder(1);
        final JBTable myOptionsTable = new JBTable(myOptionsTableModel);

        def decorator = ToolbarDecorator.createDecorator(
            myCheckersTable
        ).addExtraAction(
            new SelectionActionButton("Select all", AllIcons.Actions.Selectall, myCheckersTable, true)
        ).addExtraAction(
            new SelectionActionButton("Unselect all", AllIcons.Actions.Unselectall, myCheckersTable, false)
        )
        def optionsTableDecorator = ToolbarDecorator.createDecorator(myOptionsTable);

        myCheckersPanel.add(decorator.createPanel(), CENTER);
        myOptionsPanel.add(optionsTableDecorator.createPanel(), CENTER);

        myCreateGlobalLibraryButton.visible = !CheckerFrameworkLibrary.exists();
        myCreateGlobalLibraryButton.addActionListener({
            CheckerFrameworkLibrary.getOrCreateLibrary();
            myCreateGlobalLibraryButton.visible = !CheckerFrameworkLibrary.exists();
        });
        myProcessorProfilesCombobox.addActionListener({
            final ProcessorConfigProfile configProfile = myProcessorProfilesCombobox.selectedItem as ProcessorConfigProfile;
            Set<String> existing = configProfile?.processors ?: [] as Set<String>
            Set<String> enabled = myCheckersTableModel.enabledClasses.collect {Class clazz -> clazz.canonicalName} as Set<String>
            myAddCheckersToSelectedProfileButton.visible = !(existing.equals(enabled))
        });
        myAddCheckersToSelectedProfileButton.addActionListener({
            final ProcessorConfigProfile configProfile = myProcessorProfilesCombobox.selectedItem as ProcessorConfigProfile;
            BUILTIN_CHECKERS.collect {
                Class clazz -> clazz.canonicalName
            }.each {
                String fqn -> configProfile?.removeProcessor(fqn)
            }
            myCheckersTableModel.enabledClasses.collect {
                Class clazz -> clazz.canonicalName
            }.each {
                String fqn -> configProfile?.addProcessor(fqn)
            }
            myOptionsTableModel.options.each {
                String option ->
                    def opts = option.split(' ')
                    configProfile?.setOption(opts[0], opts.tail().join(" "))
            }
            configProfile?.obtainProcessorsFromClasspath = false
            configProfile?.processorPath = Stuff.PATH_TO_CHECKER
            configProfile?.enabled = true
            myAddCheckersToSelectedProfileButton.visible = false
        });
    }

    private static class SelectionActionButton extends AnActionButton {

        JBTable table;
        boolean value;
        int column;

        SelectionActionButton(String text, Icon icon, JBTable table, boolean value) {
            this(text, icon, table, value, 0)
        }

        SelectionActionButton(String text, Icon icon, JBTable table, boolean value, int column) {
            super(text, icon)
            this.table = table
            this.value = value
            this.column = column
        }

        @Override
        public void actionPerformed(AnActionEvent e) {
            for (i in 0..<table.rowCount) {
                table.setValueAt(value, i, column)
            }
        }
    }
}
