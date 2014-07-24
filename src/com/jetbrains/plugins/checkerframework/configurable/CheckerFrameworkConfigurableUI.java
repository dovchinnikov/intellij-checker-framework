package com.jetbrains.plugins.checkerframework.configurable;

import com.intellij.ide.IdeBundle;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.fileChooser.FileChooser;
import com.intellij.openapi.fileChooser.FileChooserDescriptor;
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.ui.ComponentWithBrowseButton;
import com.intellij.openapi.ui.TextComponentAccessor;
import com.intellij.openapi.ui.TextFieldWithBrowseButton;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.vfs.VfsUtilCore;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.platform.templates.github.ZipUtil;
import com.intellij.ui.HyperlinkLabel;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.table.JBTable;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.download.DownloadableFileService;
import com.intellij.util.ui.UIUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.event.DocumentListener;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;
import javax.swing.table.TableModel;
import java.awt.event.ActionEvent;
import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.regex.Pattern;

public abstract class CheckerFrameworkConfigurableUI {

    private static final Logger LOG = Logger.getInstance(CheckerFrameworkConfigurableUI.class);
    private static final FileChooserDescriptor JAR_DESCRIPTOR = new FileChooserDescriptor(false, false, true, true, false, false);
    private static final String CHECKER_FILE_NAME = "checker-framework.zip";
    //private static final String CHECKER_DOWNLOAD_URL = "file:///home/user/Downloads/checker-framework.zip";
    private static final String CHECKER_DOWNLOAD_URL = "http://types.cs.washington.edu/checker-framework/current/" + CHECKER_FILE_NAME;

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
        myDownloadCheckerLink.addHyperlinkListener(new DownloadCheckerFrameworkLinkListener());
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

    private class DownloadCheckerFrameworkLinkListener implements HyperlinkListener {

        @Override
        public void hyperlinkUpdate(HyperlinkEvent event) {
            final File directoryToUnpackTo;
            final VirtualFile virtualDir = chooseDirectoryForFiles(myRootPane);
            if (virtualDir == null) {
                LOG.debug("Directory was not selected");
                return;
            }
            directoryToUnpackTo = VfsUtilCore.virtualToIoFile(virtualDir);

            final String directoryToDownloadTo;
            try {
                directoryToDownloadTo = FileUtil.createTempDirectory("checker-framework", "idea").getAbsolutePath();
            } catch (IOException e) {
                LOG.error(e);
                return;
            }

            final DownloadableFileService downloadableFileService = DownloadableFileService.getInstance();
            final VirtualFile frameworkArchive = ContainerUtil.getFirstItem(
                downloadableFileService.createDownloader(
                    Collections.singletonList(
                        downloadableFileService.createFileDescription(
                            CHECKER_DOWNLOAD_URL,
                            CHECKER_FILE_NAME
                        )
                    ),
                    CHECKER_FILE_NAME
                ).downloadFilesWithProgress(
                    directoryToDownloadTo,
                    null,
                    myRootPane
                )
            );
            if (frameworkArchive == null) {
                LOG.warn("Cannot download " + CHECKER_DOWNLOAD_URL);
                return;
            }

            ProgressManager.getInstance().run(new Task.Modal(null, "Unpacking " + CHECKER_FILE_NAME + "...", false) {
                @Override
                public void run(@NotNull ProgressIndicator indicator) {
                    try {
                        ZipUtil.unzip(indicator, directoryToUnpackTo, VfsUtilCore.virtualToIoFile(frameworkArchive), null, null, false);
                    } catch (IOException e) {
                        LOG.error("Cannot unpack " + frameworkArchive.getPath(), e);
                    }
                }
            });

            final File checkerJar = ContainerUtil.getFirstItem(
                FileUtil.findFilesByMask(
                    Pattern.compile(".*checker\\.jar$"),
                    directoryToUnpackTo
                )
            );
            if (checkerJar == null) {
                LOG.error("Cannot find checker.jar");
                return;
            }

            myPathToCheckerJarField.setText(checkerJar.getAbsolutePath());
        }

        /**
         * Copied from {@link com.intellij.util.download.impl.FileDownloaderImpl#chooseDirectoryForFiles()}
         */
        @Nullable
        private VirtualFile chooseDirectoryForFiles(@Nullable JComponent parentComponent) {
            final FileChooserDescriptor descriptor = FileChooserDescriptorFactory.createSingleFolderDescriptor();
            //noinspection DialogTitleCapitalization
            descriptor.setTitle(IdeBundle.message("dialog.directory.for.downloaded.files.title"));
            return FileChooser.chooseFile(descriptor, parentComponent, null, null);
        }
    }
}
