/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */
package com.microsoft.azure.toolkit.intellij.cosmos.actions;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.intellij.openapi.application.WriteAction;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.microsoft.azure.toolkit.intellij.common.fileexplorer.VirtualFileActions;
import com.microsoft.azure.toolkit.lib.common.bundle.AzureString;
import com.microsoft.azure.toolkit.lib.common.messager.AzureMessager;
import com.microsoft.azure.toolkit.lib.common.operation.AzureOperation;
import com.microsoft.azure.toolkit.lib.common.operation.OperationBundle;
import com.microsoft.azure.toolkit.lib.common.task.AzureTask;
import com.microsoft.azure.toolkit.lib.common.task.AzureTaskManager;
import com.microsoft.azure.toolkit.lib.cosmos.ICosmosDocument;
import lombok.SneakyThrows;
import org.apache.commons.io.FilenameUtils;

import java.io.File;
import java.util.Objects;
import java.util.function.Function;

public class OpenCosmosDocumentAction {
    private static final String APP_SERVICE_FILE_EDITING = "App Service File Editing";
    private static final String FILE_HAS_BEEN_DELETED = "File '%s' has been deleted from remote server, " +
            "do you want to create a new file with the changed content?";
    private static final String FILE_HAS_BEEN_MODIFIED = "File '%s' has been modified since you view it, do you still want to save your changes?";
    private static final String SAVE_CHANGES = "Do you want to save your changes?";
    private static final Key<String> DOCUMENT_FILE_ID = new Key<>("DOCUMENT_FILE_ID");
    private static final String ERROR_DOWNLOADING = "Failed to download file[%s] to [%s].";
    private static final String SUCCESS_DOWNLOADING = "File[%s] is successfully downloaded to [%s].";
    private static final String NOTIFICATION_GROUP_ID = "Azure Plugin";
    private static final String FILE_HAS_BEEN_SAVED = "File %s has been saved to Azure";

    @SneakyThrows
    public static void open(ICosmosDocument target, Project project) {
        final FileEditorManager fileEditorManager = FileEditorManager.getInstance(project);
        final VirtualFile virtualFile = getOrCreateVirtualFile(target, fileEditorManager);
        final Function<String, Boolean> onSave = content -> {
            AzureTaskManager.getInstance().runInBackground(new AzureTask<>(OperationBundle.description("internal/cosmos.update_document.document", target.getName()), () -> {
                try {
                    final ObjectNode node = new ObjectMapper().readValue(content, ObjectNode.class);
                    target.updateDocument(node);
                    AzureMessager.getMessager().info(String.format("Save document %s to Azure successfully.", virtualFile.getName()));
                } catch (final RuntimeException | JsonProcessingException e) {
                    AzureMessager.getMessager().error(e);
                }
            }));
            return true;
        };
        final Runnable onClose = () -> WriteAction.run(() -> FileUtil.delete(new File(virtualFile.getPath())));
        final AzureString title = OperationBundle.description("user/$appservice.open_file.file", virtualFile.getName());
        AzureTaskManager.getInstance().runLater(new AzureTask<>(title, () -> VirtualFileActions.openFileInEditor(virtualFile, onSave, onClose, fileEditorManager)));
    }

    private static synchronized VirtualFile getOrCreateVirtualFile(final ICosmosDocument document, final FileEditorManager manager) {
        final VirtualFile virtualFile = VirtualFileActions.getVirtualFile(document.getId(), manager);
        return Objects.isNull(virtualFile) ? createVirtualFile(document, manager) : virtualFile;
    }

    @SneakyThrows
    private static VirtualFile createVirtualFile(final ICosmosDocument document, FileEditorManager manager) {
        final File tempFile = FileUtil.createTempFile(document.getName(), ".json", true);
        FileUtil.writeToFile(tempFile, Objects.requireNonNull(document.getDocument(), String.format("Failed to create temp file for document %s", document.getName())).toPrettyString());
        final String virtualFileName = String.format("%s.%s", document.getName(), FilenameUtils.getExtension(tempFile.getName()));
        return VirtualFileActions.createVirtualFile(document.getId(), virtualFileName, tempFile, manager);
    }
}
