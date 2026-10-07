package com.microsoft.azure.toolkit.intellij.cosmos.actions;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.intellij.openapi.fileChooser.FileChooserDescriptor;
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory;
import com.intellij.openapi.fileChooser.FileChooserDialog;
import com.intellij.openapi.fileChooser.FileChooserFactory;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.microsoft.azure.toolkit.lib.common.messager.AzureMessager;
import com.microsoft.azure.toolkit.lib.common.task.AzureTaskManager;
import com.microsoft.azure.toolkit.lib.cosmos.ICosmosDocument;
import com.microsoft.azure.toolkit.lib.cosmos.ICosmosDocumentContainer;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.io.InputStream;

public class UploadCosmosDocumentAction {
    public static void importDocument(@Nonnull ICosmosDocumentContainer<?> container, @Nonnull Project project) {
        final FileChooserDescriptor json = FileChooserDescriptorFactory.createSingleFileDescriptor("json");
        json.setTitle("Select the document to import");
        final VirtualFile[] virtualFiles = AzureTaskManager.getInstance().runLater(() -> {
            final FileChooserDialog fileChooser = FileChooserFactory.getInstance().createFileChooser(json, project, null);
            return fileChooser.choose(project, LocalFileSystem.getInstance().findFileByPath(System.getProperty("user.home")));
        }).join();
        if (virtualFiles != null && virtualFiles.length > 0) {
            try (final InputStream stream = virtualFiles[0].getInputStream()) {
                final ObjectNode jsonNodes = new ObjectMapper().readValue(stream, ObjectNode.class);
                final ICosmosDocument sqlDocument = container.importDocument(jsonNodes);
            } catch (IOException e) {
                AzureMessager.getMessager().error(e);
            }
        }
    }
}
