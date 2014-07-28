package com.jetbrains.plugins.checkerframework.configurable;

import com.intellij.ide.IdeBundle;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.fileChooser.FileChooser;
import com.intellij.openapi.fileChooser.FileChooserDescriptor;
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.vfs.VfsUtilCore;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.platform.templates.github.ZipUtil;
import com.intellij.ui.TextAccessor;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.download.DownloadableFileService;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;
import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.regex.Pattern;

public class DownloadCheckerFrameworkLinkListener implements HyperlinkListener {

    private static final String CHECKER_FILE_NAME = "checker-framework.zip";
    //private static final String CHECKER_DOWNLOAD_URL = "file:///home/user/Downloads/checker-framework.zip";
    private static final String CHECKER_DOWNLOAD_URL = "http://types.cs.washington.edu/checker-framework/current/" + CHECKER_FILE_NAME;

    private static final Logger LOG = Logger.getInstance(DownloadCheckerFrameworkLinkListener.class);
    private final JComponent myRootPane;
    private final TextAccessor myPathToCheckerJarField;

    /**
     * @param rootPane              parent component for the progress window.
     * @param pathToCheckerJarField field to set path to.
     */
    public DownloadCheckerFrameworkLinkListener(final JComponent rootPane, final TextAccessor pathToCheckerJarField) {
        myRootPane = rootPane;
        myPathToCheckerJarField = pathToCheckerJarField;
    }

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
    @SuppressWarnings({"JavadocReference", "DialogTitleCapitalization"})
    @Nullable
    private static VirtualFile chooseDirectoryForFiles(@Nullable JComponent parentComponent) {
        final FileChooserDescriptor descriptor = FileChooserDescriptorFactory.createSingleFolderDescriptor();
        descriptor.setTitle(IdeBundle.message("dialog.directory.for.downloaded.files.title"));
        return FileChooser.chooseFile(descriptor, parentComponent, null, null);
    }
}
