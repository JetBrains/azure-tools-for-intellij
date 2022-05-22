/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Copyright (c) 2018-2021 JetBrains s.r.o.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.intellij.helpers;

import com.google.common.collect.ImmutableMap;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.fileChooser.FileChooserFactory;
import com.intellij.openapi.fileChooser.FileSaverDescriptor;
import com.intellij.openapi.fileChooser.FileSaverDialog;
import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.MessageType;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.util.IconLoader;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileWrapper;
import com.intellij.openapi.wm.StatusBar;
import com.intellij.openapi.wm.WindowManager;
import com.intellij.testFramework.LightVirtualFile;
import com.intellij.util.ui.UIUtil;
import com.microsoft.azure.CloudError;
import com.microsoft.azure.CloudException;
import com.microsoft.azure.management.appplatform.v2020_07_01.implementation.AppResourceInner;
import com.microsoft.azure.management.storage.StorageAccount;
import com.microsoft.azure.toolkit.intellij.arm.DeploymentPropertyView;
import com.microsoft.azure.toolkit.intellij.arm.ResourceTemplateView;
import com.microsoft.azure.toolkit.intellij.arm.ResourceTemplateViewProvider;
import com.microsoft.azure.toolkit.intellij.function.FunctionAppPropertyViewProvider;
import com.microsoft.azure.toolkit.intellij.mysql.MySQLPropertyView;
import com.microsoft.azure.toolkit.intellij.mysql.MySQLPropertyViewProvider;
import com.microsoft.azure.toolkit.intellij.redis.RedisCacheExplorerProvider;
import com.microsoft.azure.toolkit.intellij.redis.RedisCachePropertyView;
import com.microsoft.azure.toolkit.intellij.redis.RedisCachePropertyViewProvider;
import com.microsoft.azure.toolkit.intellij.webapp.DeploymentSlotPropertyViewProvider;
import com.microsoft.azure.toolkit.intellij.webapp.WebAppPropertyViewProvider;
import com.microsoft.azure.toolkit.lib.common.task.AzureTaskManager;
import com.microsoft.azuretools.ActionConstants;
import com.microsoft.azuretools.authmanage.AuthMethodManager;
import com.microsoft.azuretools.azurecommons.helpers.AzureCmdException;
import com.microsoft.azuretools.azurecommons.helpers.NotNull;
import com.microsoft.azuretools.azurecommons.helpers.Nullable;
import com.microsoft.azuretools.azurecommons.util.Utils;
import com.microsoft.azuretools.telemetry.TelemetryConstants;
import com.microsoft.azuretools.telemetrywrapper.EventUtil;
import com.microsoft.intellij.AzurePlugin;
import com.microsoft.intellij.forms.ErrorMessageForm;
import com.microsoft.intellij.forms.OpenSSLFinderForm;
import com.microsoft.intellij.helpers.storage.*;
import com.microsoft.intellij.ui.util.UIUtils;
import com.microsoft.intellij.util.PluginUtil;
import com.microsoft.tooling.msservices.components.DefaultLoader;
import com.microsoft.tooling.msservices.helpers.UIHelper;
import com.microsoft.tooling.msservices.model.storage.Queue;
import com.microsoft.tooling.msservices.model.storage.*;
import com.microsoft.tooling.msservices.serviceexplorer.AzureIconSymbol;
import com.microsoft.tooling.msservices.serviceexplorer.Node;
import com.microsoft.tooling.msservices.serviceexplorer.azure.arm.deployments.DeploymentNode;
import com.microsoft.tooling.msservices.serviceexplorer.azure.container.ContainerRegistryNode;
import com.microsoft.tooling.msservices.serviceexplorer.azure.function.FunctionAppNode;
import com.microsoft.tooling.msservices.serviceexplorer.azure.function.deploymentslot.FunctionDeploymentSlotNode;
import com.microsoft.tooling.msservices.serviceexplorer.azure.mysql.MySQLNode;
import com.microsoft.tooling.msservices.serviceexplorer.azure.rediscache.RedisCacheNode;
import com.microsoft.tooling.msservices.serviceexplorer.azure.webapp.WebAppNode;
import com.microsoft.tooling.msservices.serviceexplorer.azure.webapp.deploymentslot.DeploymentSlotNode;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.NotImplementedException;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;
import java.util.concurrent.RunnableFuture;
import java.util.function.Supplier;

import static com.microsoft.azure.toolkit.intellij.arm.DeploymentPropertyViewProvider.TYPE;


public class UIHelperImpl implements UIHelper {
    public static Key<StorageAccount> STORAGE_KEY = new Key<>("storageAccount");
    public static Key<ClientStorageAccount> CLIENT_STORAGE_KEY = new Key<ClientStorageAccount>("clientStorageAccount");
    public static final Key<String> SUBSCRIPTION_ID = new Key<>("subscriptionId");
    public static final Key<String> RESOURCE_ID = new Key<>("resourceId");
    public static final Key<String> WEBAPP_ID = new Key<>("webAppId");
    public static final Key<String> FUNCTIONAPP_ID = new Key<>("functionAppId");
    public static final Key<String> APP_ID = new Key<>("appId");
    public static final Key<String> CLUSTER_ID = new Key<>("clusterId");
    public static final Key<AppResourceInner> SPRING_CLOUD_APP = new Key<>("springCloudApp");

    public static final Key<String> SLOT_NAME = new Key<>("slotName");
    private Map<Class<? extends StorageServiceTreeItem>, Key<? extends StorageServiceTreeItem>> name2Key =
        ImmutableMap.of(BlobContainer.class, BlobExplorerFileEditorProvider.CONTAINER_KEY,
                        Queue.class, QueueExplorerFileEditorProvider.QUEUE_KEY,
                        Table.class, TableExplorerFileEditorProvider.TABLE_KEY);

    private static final String UNABLE_TO_OPEN_BROWSER = "Unable to open external web browser";
    protected static final String UNABLE_TO_OPEN_EDITOR_WINDOW = "Unable to open new editor window";
    protected static final String CANNOT_GET_FILE_EDITOR_MANAGER = "Cannot get FileEditorManager";

    @Override
    public void showException(@NotNull final String message,
                              @Nullable final Throwable ex,
                              @NotNull final String title,
                              final boolean appendEx,
                              final boolean suggestDetail) {

        if (tryNotifyUserRetryableException(message, ex, title)) {
            return;
        }

        ApplicationManager.getApplication().invokeLater(() -> {
            String headerMessage = getHeaderMessage(message, ex, appendEx, suggestDetail);

            String details = getDetails(ex);

            ErrorMessageForm em = new ErrorMessageForm(title);
            em.showErrorMessageForm(headerMessage, details);
            em.show();
        });
    }

    private boolean tryNotifyUserRetryableException(
            final String message,
            final Throwable ex,
            final String title) {

        if (ex instanceof CloudException) {
            final CloudException cloudException = (CloudException)ex;

            boolean isSignedIn = false;
            try {
                isSignedIn = AuthMethodManager.getInstance().isSignedIn();
            } catch (Exception e) {
                // Intentionally ignoring
            }

            // Exceptions similar to https://github.com/JetBrains/azure-tools-for-intellij/issues/436
            // may be user-retryable. Do further analysis...
            // Example: {"error":{"code":"InvalidAuthenticationToken","message":"The access token is invalid."}}
            if (!isSignedIn && cloudException.response().code() == 401) {
                final CloudError cloudError = cloudException.body();
                if (cloudError != null && cloudError.code() != null && "InvalidAuthenticationToken".equals(cloudError.code())) {
                    // Skip report error form, show a notification instead.
                    ApplicationManager.getApplication().invokeLater(() -> {
                        showErrorNotification(
                                cloudError.message(),
                                "Authorization is required to use Azure resources. Please sign out and sign in again.");
                    });

                    logWarning(cloudError.message(), ex);

                    return true;
                }
            }
        }

        return false;
    }

    @Override
    public void showError(@NotNull final String message, @NotNull final String title) {
        showError(null, message, title);
    }

    @Override
    public void showError(Component component, String message, String title) {
        AzureTaskManager.getInstance().runLater(() -> Messages.showErrorDialog(component, message, title));
    }

    @Override
    public boolean showConfirmation(@NotNull String message, @NotNull String title, @NotNull String[] options,
                                    String defaultOption) {
        return runFromDispatchThread(() -> 0 == Messages.showDialog(message,
                                                                    title,
                                                                    options,
                                                                    ArrayUtils.indexOf(options, defaultOption),
                                                                    null));
    }

    @Override
    public boolean showConfirmation(@NotNull Component node, @NotNull String message, @NotNull String title, @NotNull String[] options, String defaultOption) {
        return runFromDispatchThread(() -> 0 == Messages.showDialog(node,
                                                                    message,
                                                                    title,
                                                                    options,
                                                                    ArrayUtils.indexOf(options, defaultOption),
                                                                    null));
    }

    @Override
    public void showInfo(Node node, String s) {
        showNotification(node, s, MessageType.INFO);
    }

    @Override
    public void showError(Node node, String s) {
        showNotification(node, s, MessageType.ERROR);
    }

    private void showNotification(Node node, String s, MessageType type) {
        StatusBar statusBar = WindowManager.getInstance().getStatusBar((Project) node.getProject());
        UIUtils.showNotification(statusBar, s, type);
    }

    @Override
    public void logError(String message, Throwable ex) {
        AzurePlugin.log(message, ex);
    }

    public void logWarning(String message, Throwable ex) {
        AzurePlugin.logWarning(message, ex);
    }

    /**
     * returns File if file chosen and OK pressed; otherwise returns null
     * TODO: name confusion, FileChooser vs FileSaver
     */
    @Override
    public File showFileChooser(String title) {
        return showFileSaver(title, "");
    }

    @Override
    public File showFileSaver(String title, String fileName) {
        FileSaverDescriptor fileDescriptor = new FileSaverDescriptor(title, "");
        final FileSaverDialog dialog = FileChooserFactory.getInstance().createSaveFileDialog(fileDescriptor, (Project) null);
        final VirtualFileWrapper save = dialog.save(LocalFileSystem.getInstance().findFileByPath(System.getProperty("user.home")), fileName);

        if (save != null) {
            return save.getFile();
        }
        return null;
    }

    @Override
    public <T extends StorageServiceTreeItem> void openItem(@NotNull Object projectObject,
                                                            @Nullable StorageAccount storageAccount,
                                                            @NotNull T item,
                                                            @Nullable String itemType,
                                                            @NotNull final String itemName,
                                                            @Nullable final String iconName) {
        LightVirtualFile itemVirtualFile = new LightVirtualFile(item.getName() + itemType);
        itemVirtualFile.putUserData((Key<T>) name2Key.get(item.getClass()), item);
        itemVirtualFile.putUserData(STORAGE_KEY, storageAccount);

        itemVirtualFile.setFileType(new AzureFileType(itemName, UIHelperImpl.loadIcon(iconName)));

        openItem(projectObject, itemVirtualFile);
    }

    @Override
    public <T extends StorageServiceTreeItem> void openItem(Object projectObject,
                                                            ClientStorageAccount clientStorageAccount,
                                                            T item, String itemType,
                                                            String itemName,
                                                            String iconName) {
        LightVirtualFile itemVirtualFile = new LightVirtualFile(item.getName() + itemType);
        itemVirtualFile.putUserData((Key<T>) name2Key.get(item.getClass()), item);
        itemVirtualFile.putUserData(CLIENT_STORAGE_KEY, clientStorageAccount);

        itemVirtualFile.setFileType(new AzureFileType(itemName, UIHelperImpl.loadIcon(iconName)));

        openItem(projectObject, itemVirtualFile);
    }

    @Override
    public void openItem(@NotNull final Object projectObject, @NotNull final Object itemVirtualFile) {
        AzureTaskManager
            .getInstance()
            .runLater(() -> FileEditorManager.getInstance((Project) projectObject).openFile((VirtualFile) itemVirtualFile, true, true));
    }

    protected class AzureFileType implements FileType {
        private String itemName;
        private Icon icon;

        AzureFileType(String itemName, Icon icon) {
            this.itemName = itemName;
            this.icon = icon;
        }

        @NotNull
        @Override
        public String getName() {
            return itemName;
        }

        @NotNull
        @Override
        public String getDescription() {
            return itemName;
        }

        @NotNull
        @Override
        public String getDefaultExtension() {
            return "";
        }

        @Nullable
        @Override
        public Icon getIcon() {
            // UIHelperImpl.loadIcon(iconName);
            return icon;
        }

        @Override
        public boolean isBinary() {
            return true;
        }

        @Override
        public boolean isReadOnly() {
            return false;
        }

        @Override
        public String getCharset(@NotNull VirtualFile virtualFile, @NotNull byte[] bytes) {
            return StandardCharsets.UTF_8.name();
        }
    }

    @Override
    public void refreshQueue(@NotNull final Object projectObject, @NotNull final ClientStorageAccount storageAccount,
                             @NotNull final Queue queue) {
        AzureTaskManager.getInstance().runLater(() -> {
            VirtualFile file = (VirtualFile) getOpenedFile(projectObject, storageAccount.getName(), queue);
            if (file != null) {
                final QueueFileEditor queueFileEditor = (QueueFileEditor) FileEditorManager.getInstance((Project) projectObject).getEditors(file)[0];
                AzureTaskManager.getInstance().runLater(() -> queueFileEditor.fillGrid());
            }
        });
    }

    @Override
    public void refreshBlobs(@NotNull final Object projectObject, @NotNull final String accountName, @NotNull final BlobContainer container) {
        AzureTaskManager.getInstance().runLater(() -> {
            VirtualFile file = (VirtualFile) getOpenedFile(projectObject, accountName, container);
            if (file != null) {
                final BlobExplorerFileEditor containerFileEditor =
                    (BlobExplorerFileEditor) FileEditorManager.getInstance((Project) projectObject)
                                                              .getEditors(file)[0];
                AzureTaskManager.getInstance().runLater(() -> containerFileEditor.fillGrid());
            }
        });
    }

    @Override
    public void refreshTable(@NotNull final Object projectObject, @NotNull final StorageAccount storageAccount,
                             @NotNull final Table table) {
        AzureTaskManager.getInstance().runLater(() -> {
            final VirtualFile file = (VirtualFile) getOpenedFile(projectObject, storageAccount.name(), table);
            if (file != null) {
                final TableFileEditor tableFileEditor = (TableFileEditor) FileEditorManager.getInstance((Project) projectObject).getEditors(file)[0];
                AzureTaskManager.getInstance().runLater(tableFileEditor::fillGrid);
            }
        });
    }

    @NotNull
    @Override
    public String promptForOpenSSLPath() {
        OpenSSLFinderForm openSSLFinderForm = new OpenSSLFinderForm(null);
        openSSLFinderForm.setModal(true);
        openSSLFinderForm.show();

        return DefaultLoader.getIdeHelper().getPropertyWithDefault("MSOpenSSLPath", "");
    }

    @Override
    public void openRedisPropertyView(@NotNull RedisCacheNode node) {
        EventUtil.executeWithLog(TelemetryConstants.REDIS, TelemetryConstants.REDIS_READPROP, (operation) -> {
            String redisName = node.getName() != null ? node.getName() : RedisCacheNode.TYPE;
            String sid = node.getSubscriptionId();
            String resId = node.getResourceId();
            if (isSubscriptionIdAndResourceIdEmpty(sid, resId)) {
                return;
            }
            Project project = (Project) node.getProject();
            FileEditorManager fileEditorManager = FileEditorManager.getInstance(project);
            if (fileEditorManager == null) {
                showError(CANNOT_GET_FILE_EDITOR_MANAGER, UNABLE_TO_OPEN_EDITOR_WINDOW);
                return;
            }
            LightVirtualFile itemVirtualFile = searchExistingFile(fileEditorManager,
                                                                  RedisCachePropertyViewProvider.TYPE, resId);
            if (itemVirtualFile == null) {
                itemVirtualFile = createVirtualFile(redisName, sid, resId);
                itemVirtualFile.setFileType(
                        new AzureFileType(RedisCachePropertyViewProvider.TYPE, AzureIconLoader.loadIcon(AzureIconSymbol.RedisCache.MODULE)));
            }
            FileEditor[] editors = fileEditorManager.openFile(itemVirtualFile, true, true);
            for (FileEditor editor : editors) {
                if (editor.getName().equals(RedisCachePropertyView.ID) &&
                    editor instanceof RedisCachePropertyView) {
                    ((RedisCachePropertyView) editor).onReadProperty(sid, resId);
                }
            }
        });
    }

    @Override
    public void openRedisExplorer(RedisCacheNode redisCacheNode) {
        String redisName = redisCacheNode.getName() != null ? redisCacheNode.getName() : RedisCacheNode.TYPE;
        String sid = redisCacheNode.getSubscriptionId();
        String resId = redisCacheNode.getResourceId();
        if (isSubscriptionIdAndResourceIdEmpty(sid, resId)) {
            return;
        }
        Project project = (Project) redisCacheNode.getProject();
        FileEditorManager fileEditorManager = FileEditorManager.getInstance(project);
        if (fileEditorManager == null) {
            showError(CANNOT_GET_FILE_EDITOR_MANAGER, UNABLE_TO_OPEN_EDITOR_WINDOW);
            return;
        }
        LightVirtualFile itemVirtualFile = searchExistingFile(fileEditorManager, RedisCacheExplorerProvider.TYPE, resId);
        if (itemVirtualFile == null) {
            itemVirtualFile = createVirtualFile(redisName, sid, resId);
            itemVirtualFile.setFileType(new AzureFileType(RedisCacheExplorerProvider.TYPE, AzureIconLoader.loadIcon(AzureIconSymbol.RedisCache.MODULE)));

        }
        fileEditorManager.openFile(itemVirtualFile, true, true);
    }

    @Override
    public void openDeploymentPropertyView(DeploymentNode node) {
        Project project = (Project) node.getProject();
        FileEditorManager fileEditorManager = FileEditorManager.getInstance(project);
        if (fileEditorManager == null) {
            showError(CANNOT_GET_FILE_EDITOR_MANAGER, UNABLE_TO_OPEN_EDITOR_WINDOW);
            return;
        }
        LightVirtualFile itemVirtualFile = searchExistingFile(fileEditorManager, TYPE, node.getId());
        if (itemVirtualFile == null) {
            itemVirtualFile = createVirtualFile(node.getName(), node.getSubscriptionId(), node.getId());
            itemVirtualFile.setFileType(new AzureFileType(TYPE, UIHelperImpl.loadIcon(DeploymentNode.ICON_PATH)));
        }
        FileEditor[] fileEditors = fileEditorManager.openFile(itemVirtualFile, true, true);
        for (FileEditor fileEditor : fileEditors) {
            if (fileEditor.getName().equals(DeploymentPropertyView.ID) && fileEditor instanceof DeploymentPropertyView) {
                ((DeploymentPropertyView) fileEditor).onLoadProperty(node);
            }
        }
    }

    @Override
    public void openResourceTemplateView(DeploymentNode node, String template) {
        Project project = (Project) node.getProject();
        FileEditorManager fileEditorManager = FileEditorManager.getInstance(project);
        if (fileEditorManager == null) {
            showError(CANNOT_GET_FILE_EDITOR_MANAGER, UNABLE_TO_OPEN_EDITOR_WINDOW);
            return;
        }
        LightVirtualFile itemVirtualFile = searchExistingFile(fileEditorManager, ResourceTemplateViewProvider.TYPE,
                                                              node.getId());
        if (itemVirtualFile == null) {
            itemVirtualFile = createVirtualFile(node.getName(), node.getSubscriptionId(), node.getId());
            itemVirtualFile.setFileType(new AzureFileType(ResourceTemplateViewProvider.TYPE, UIHelperImpl.loadIcon(DeploymentNode.ICON_PATH)));
        }
        FileEditor[] fileEditors = fileEditorManager.openFile(itemVirtualFile, true, true);
        for (FileEditor fileEditor : fileEditors) {
            if (fileEditor.getName().equals(ResourceTemplateView.ID) && fileEditor instanceof ResourceTemplateView) {
                ((ResourceTemplateView) fileEditor).loadTemplate(node, template);
            }
        }
    }

    @Override
    public void openInBrowser(String link) {
        try {
            Desktop.getDesktop().browse(URI.create(link));
        } catch (Throwable e) {
            throw new RuntimeException(UNABLE_TO_OPEN_BROWSER, e);
        }
    }

    @Override
    public void openContainerRegistryPropertyView(@NotNull ContainerRegistryNode node) {
        throw new NotImplementedException("Must be defined by inheritors");
    }

    protected FileEditorManager getFileEditorManager(@NotNull final String sid, @NotNull final String webAppId,
                                                     @NotNull final Project project) {
        if (isSubscriptionIdAndResourceIdEmpty(sid, webAppId)) {
            return null;
        }
        final FileEditorManager fileEditorManager = FileEditorManager.getInstance(project);
        if (fileEditorManager == null) {
            showError(CANNOT_GET_FILE_EDITOR_MANAGER, UNABLE_TO_OPEN_EDITOR_WINDOW);
            return null;
        }
        return fileEditorManager;
    }

    @Override
    public void openWebAppPropertyView(@NotNull final WebAppNode node) {
        final String sid = node.getSubscriptionId();
        final String webAppId = node.getWebAppId();
        final FileEditorManager fileEditorManager = getFileEditorManager(sid, webAppId, (Project) node.getProject());
        if (fileEditorManager == null) {
            return;
        }
        final String type = WebAppPropertyViewProvider.TYPE;
        LightVirtualFile itemVirtualFile = searchExistingFile(fileEditorManager, type, webAppId);
        if (itemVirtualFile == null) {
            itemVirtualFile = createVirtualFile(node.getWebAppName(), sid, webAppId);
            itemVirtualFile.setFileType(new AzureFileType(type, AzureIconLoader.loadIcon(AzureIconSymbol.WebApp.MODULE)));

        }

        final LightVirtualFile finalItemVirtualFile = itemVirtualFile;
        AzureTaskManager.getInstance().runLater(
            () -> fileEditorManager.openFile(finalItemVirtualFile, true /*focusEditor*/, true /*searchForOpen*/));
    }

    @Override
    public void openDeploymentSlotPropertyView(@NotNull DeploymentSlotNode node) {
        final String sid = node.getSubscriptionId();
        final String resourceId = node.getId();
        final FileEditorManager fileEditorManager = getFileEditorManager(sid, resourceId, (Project) node.getProject());
        if (fileEditorManager == null) {
            return;
        }
        final String type = DeploymentSlotPropertyViewProvider.TYPE;

        LightVirtualFile itemVirtualFile = searchExistingFile(fileEditorManager, type, resourceId);
        if (itemVirtualFile == null) {
            final String iconPath = node.getParent() == null ? node.getIconPath()
                                                             : node.getParent().getIconPath();
            final Map<Key, String> userData = new HashMap<>();
            userData.put(SUBSCRIPTION_ID, sid);
            userData.put(RESOURCE_ID, resourceId);
            userData.put(WEBAPP_ID, node.getAppId());
            userData.put(SLOT_NAME, node.getName());
            itemVirtualFile = createVirtualFile(node.getAppName() + "-" + node.getName(), userData);
            itemVirtualFile.setFileType(new AzureFileType(type, AzureIconLoader.loadIcon(AzureIconSymbol.DeploymentSlot.MODULE)));
        }

        final LightVirtualFile finalItemVirtualFile = itemVirtualFile;
        AzureTaskManager.getInstance().runLater(
            () -> fileEditorManager.openFile(finalItemVirtualFile, true /*focusEditor*/, true /*searchForOpen*/));
    }

    /**
     * Add FunctionDeploymentSlotNode action to show deployment slot property.
     * Left the original code for [DeploymentSlotNode] unchanged to simplify merging upstream into the repo.
     *
     * @param node - node represents function deployment slot
     */
    @Override
    public void openDeploymentSlotPropertyView(@NotNull final FunctionDeploymentSlotNode node) {
        final String sid = node.getSubscriptionId();
        final String resourceId = node.getId();
        final FileEditorManager fileEditorManager = getFileEditorManager(sid, resourceId, (Project) node.getProject());
        if (fileEditorManager == null) {
            return;
        }
        final String type = DeploymentSlotPropertyViewProvider.TYPE;

        LightVirtualFile itemVirtualFile = searchExistingFile(fileEditorManager, type, resourceId);
        if (itemVirtualFile == null) {
            final String iconPath = node.getParent() == null ? node.getIconPath()
                                                             : node.getParent().getIconPath();
            final Map<Key, String> userData = new HashMap<>();
            userData.put(SUBSCRIPTION_ID, sid);
            userData.put(RESOURCE_ID, resourceId);
            userData.put(FUNCTIONAPP_ID, node.getAppId());
            userData.put(SLOT_NAME, node.getName());
            itemVirtualFile = createVirtualFile(node.getAppName() + "-" + node.getName(), userData);
            itemVirtualFile.setFileType(new AzureFileType(type, AzureIconLoader.loadIcon(AzureIconSymbol.FunctionApp.MODULE)));
        }

        final LightVirtualFile finalItemVirtualFile = itemVirtualFile;
        AzureTaskManager.getInstance().runLater(
            () -> fileEditorManager.openFile(finalItemVirtualFile, true, true));
    }

    @Override
    public void openFunctionAppPropertyView(FunctionAppNode functionNode) {
        final String subscriptionId = functionNode.getSubscriptionId();
        final String functionAppId = functionNode.getFunctionAppId();
        final FileEditorManager fileEditorManager = getFileEditorManager(subscriptionId, functionAppId, (Project) functionNode.getProject());
        if (fileEditorManager == null) {
            return;
        }
        final String type = FunctionAppPropertyViewProvider.TYPE;
        LightVirtualFile itemVirtualFile = searchExistingFile(fileEditorManager, type, functionAppId);
        if (itemVirtualFile == null) {
            final String iconPath = functionNode.getParent() == null ? functionNode.getIconPath()
                                                                     : functionNode.getParent().getIconPath();
            itemVirtualFile = createVirtualFile(functionNode.getFunctionAppName(), subscriptionId, functionAppId);
            itemVirtualFile.setFileType(new AzureFileType(type, AzureIconLoader.loadIcon(AzureIconSymbol.FunctionApp.MODULE)));
        }

        final LightVirtualFile finalItemVirtualFile = itemVirtualFile;
        AzureTaskManager.getInstance().runLater(
            () -> fileEditorManager.openFile(finalItemVirtualFile, true /*focusEditor*/, true /*searchForOpen*/));
    }

    @Override
    public void openMySQLPropertyView(@NotNull MySQLNode node) {
        EventUtil.executeWithLog(ActionConstants.MySQL.SHOW_PROPERTIES, (operation) -> {
            String name = node.getName();
            String subscriptionId = node.getSubscriptionId();
            String nodeId = node.getId();
            final FileEditorManager fileEditorManager = getFileEditorManager(subscriptionId, nodeId, (Project) node.getProject());
            if (fileEditorManager == null) {
                return;
            }
            LightVirtualFile itemVirtualFile = searchExistingFile(fileEditorManager, MySQLPropertyViewProvider.TYPE, nodeId);
            if (itemVirtualFile == null) {
                itemVirtualFile = createVirtualFile(name, subscriptionId, nodeId);
                itemVirtualFile.setFileType(new AzureFileType(MySQLPropertyViewProvider.TYPE, AzureIconLoader.loadIcon(AzureIconSymbol.MySQL.MODULE)));
            }
            FileEditor[] editors = fileEditorManager.openFile(itemVirtualFile, true, true);
            for (FileEditor editor : editors) {
                if (editor.getName().equals(MySQLPropertyView.ID) && editor instanceof MySQLPropertyView) {
                    ((MySQLPropertyView) editor).onReadProperty(subscriptionId, node.getServer().resourceGroupName(), node.getServer().name());
                }
            }
        });
    }

    @Nullable
    @Override
    public <T extends StorageServiceTreeItem> Object getOpenedFile(@NotNull Object projectObject,
                                                                   @NotNull String accountName,
                                                                   @NotNull T item) {
        FileEditorManager fileEditorManager = FileEditorManager.getInstance((Project) projectObject);

        for (VirtualFile editedFile : fileEditorManager.getOpenFiles()) {
            T editedItem = editedFile.getUserData((Key<T>) name2Key.get(item.getClass()));
            StorageAccount editedStorageAccount = editedFile.getUserData(STORAGE_KEY);
            ClientStorageAccount editedClientStorageAccount = editedFile.getUserData(CLIENT_STORAGE_KEY);
            if (((editedStorageAccount != null && editedStorageAccount.name().equals(accountName))
                || (editedClientStorageAccount != null && editedClientStorageAccount.getName().equals(accountName)))
                && editedItem != null
                && editedItem.getName().equals(item.getName())) {
                return editedFile;
            }
        }

        return null;
    }

    @Override
    public boolean isDarkTheme() {
        return UIUtil.isUnderDarcula();
    }

    @NotNull
    private static String getHeaderMessage(@NotNull String message, @Nullable Throwable ex,
                                           boolean appendEx, boolean suggestDetail) {
        String headerMessage = message.trim();

        if (ex != null && appendEx) {
            String exMessage = (ex.getLocalizedMessage() == null || ex.getLocalizedMessage().isEmpty()) ? ex.getMessage() : ex.getLocalizedMessage();
            String separator = headerMessage.matches("^.*\\d$||^.*\\w$") ? ". " : " ";
            headerMessage = headerMessage + separator + exMessage;
        }

        if (suggestDetail) {
            String separator = headerMessage.matches("^.*\\d$||^.*\\w$") ? ". " : " ";
            headerMessage = headerMessage + separator + "Click on '" +
                ErrorMessageForm.advancedInfoText + "' for detailed information on the cause of the error.";
        }

        return headerMessage;
    }

    @NotNull
    private static String getDetails(@Nullable Throwable ex) {
        String details = "";

        if (ex != null) {
            StringWriter sw = new StringWriter();
            ex.printStackTrace(new PrintWriter(sw));
            details = sw.toString();

            if (ex instanceof AzureCmdException) {
                String errorLog = ((AzureCmdException) ex).getErrorLog();
                if (errorLog != null && !errorLog.isEmpty()) {
                    details = errorLog;
                }
            }
        }

        return details;
    }

    @NotNull
    public static Icon loadIcon(@Nullable String name) {
        return IconLoader.getIcon("/icons/" + name);
    }

    protected LightVirtualFile searchExistingFile(FileEditorManager fileEditorManager, String fileType, String resourceId) {
        LightVirtualFile virtualFile = null;
        for (VirtualFile editedFile : fileEditorManager.getOpenFiles()) {
            String fileResourceId = editedFile.getUserData(RESOURCE_ID);
            if (fileResourceId != null && fileResourceId.equals(resourceId) &&
                editedFile.getFileType().getName().equals(fileType)) {
                virtualFile = (LightVirtualFile) editedFile;
                break;
            }
        }
        return virtualFile;
    }

    private LightVirtualFile createVirtualFile(String name, Map<Key, String> userData) {
        LightVirtualFile itemVirtualFile = new LightVirtualFile(name);
        for (final Map.Entry<Key, String> data : userData.entrySet()) {
            itemVirtualFile.putUserData(data.getKey(), data.getValue());
        }
        return itemVirtualFile;
    }

    protected LightVirtualFile createVirtualFile(String name, String sid, String resId) {
        LightVirtualFile itemVirtualFile = new LightVirtualFile(name);
        itemVirtualFile.putUserData(SUBSCRIPTION_ID, sid);
        itemVirtualFile.putUserData(RESOURCE_ID, resId);
        return itemVirtualFile;
    }

    protected boolean isSubscriptionIdAndResourceIdEmpty(String sid, String resId) {
        if (Utils.isEmptyString(sid)) {
            showError("Cannot get Subscription ID", UNABLE_TO_OPEN_EDITOR_WINDOW);
            return true;
        }
        if (Utils.isEmptyString(resId)) {
            showError("Cannot get resource ID", UNABLE_TO_OPEN_EDITOR_WINDOW);
            return true;
        }
        return false;
    }

    public static String readableFileSize(long size) {
        if (size <= 0) {
            return "0";
        }
        final String[] units = new String[]{"B", "kB", "MB", "GB", "TB"};
        int digitGroups = (int) (Math.log10(size) / Math.log10(1024));
        return new DecimalFormat("#,##0.#").format(size / Math.pow(1024, digitGroups)) + " " + units[digitGroups];
    }

    @Override
    public void showMessageDialog(Component component, String message, String title, Icon icon) {
        DefaultLoader.getIdeHelper().invokeLater(() -> Messages.showMessageDialog(component, message, title, icon));
    }

    @Override
    public int showConfirmDialog(Component component, String message, String title, String[] options,
                                 String defaultOption, Icon icon) {
        return runFromDispatchThread(() -> Messages.showDialog(component,
                                                               message,
                                                               title,
                                                               options,
                                                               ArrayUtils.indexOf(options, defaultOption),
                                                               icon));
    }

    @Override
    public boolean showYesNoDialog(Component component, String message, String title, Icon icon) {
        return runFromDispatchThread(() -> {
            return component == null ? Messages.showYesNoDialog(message, title, icon) == Messages.YES :
                   Messages.showYesNoDialog(component, message, title, icon) == Messages.YES;
        });
    }

    @Override
    public String showInputDialog(Component component, String message, String title, Icon icon) {
        return runFromDispatchThread(() -> Messages.showInputDialog(component, message, title, icon));
    }

    @Override
    public void showInfoNotification(String title, String message) {
        PluginUtil.showInfoNotification(title, message);
    }

    @Override
    public void showWarningNotification(String title, String message) {
        PluginUtil.showWarnNotification(title, message);
    }

    @Override
    public void showErrorNotification(String title, String message) {
        PluginUtil.showErrorNotification(title, message);
    }

    private static <T> T runFromDispatchThread(Supplier<T> supplier) {
        if (ApplicationManager.getApplication().isDispatchThread()) {
            return supplier.get();
        }
        RunnableFuture<T> runnableFuture = new FutureTask<>(() -> supplier.get());
        AzureTaskManager.getInstance().runLater(runnableFuture);
        try {
            return runnableFuture.get();
        } catch (InterruptedException | ExecutionException e) {
            return null;
        }
    }
}
