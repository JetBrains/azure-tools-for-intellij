/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Copyright (c) 2018-2021 JetBrains s.r.o.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.tooling.msservices.serviceexplorer.azure.storage;

import com.microsoft.azure.CommonIcons;
import com.microsoft.azure.management.resources.fluentcore.arm.ResourceId;
import com.microsoft.azure.management.storage.StorageAccount;
import com.microsoft.azuretools.azurecommons.helpers.AzureCmdException;
import com.microsoft.azuretools.azurecommons.helpers.Nullable;
import com.microsoft.azuretools.telemetry.AppInsightsConstants;
import com.microsoft.azuretools.telemetry.TelemetryProperties;
import com.microsoft.tooling.msservices.components.DefaultLoader;
import com.microsoft.tooling.msservices.helpers.azure.sdk.StorageClientSDKManager;
import com.microsoft.tooling.msservices.model.storage.BlobContainer;
import com.microsoft.tooling.msservices.model.storage.ClientStorageAccount;
import com.microsoft.tooling.msservices.serviceexplorer.*;
import com.microsoft.tooling.msservices.serviceexplorer.azure.AzureNodeActionPromptListener;

import java.util.HashMap;
import java.util.Map;

import static com.microsoft.azuretools.telemetry.TelemetryConstants.DELETE_BLOB_CONTAINER;
import static com.microsoft.azuretools.telemetry.TelemetryConstants.STORAGE;

public class ContainerNode extends RefreshableNode implements TelemetryProperties{

    private static final String ACTION_DELETE = "Delete";
    private static final String ACTION_VIEW_BLOB_CONTAINER = "View Blob Container";

    @Override
    public Map<String, String> toProperties() {
        final Map<String, String> properties = new HashMap<>();
        if (this.storageAccount != null) {
            properties.put(AppInsightsConstants.SubscriptionId, ResourceId.fromString(this.storageAccount.id()).subscriptionId());
            properties.put(AppInsightsConstants.Region, this.storageAccount.regionName());
        }
        return properties;
    }

    public class ViewBlobContainer extends NodeActionListener {
        @Override
        public void actionPerformed(NodeActionEvent e) {
            onNodeClick(e);
        }
    }

    public class DeleteBlobContainer extends AzureNodeActionPromptListener {
        public DeleteBlobContainer() {
            super(ContainerNode.this,
                    String.format("Are you sure you want to delete the blob container \"%s\"?", blobContainer.getName()),
                    "Deleting Blob Container");
        }

        @Override
        protected void azureNodeAction(NodeActionEvent e)
                throws AzureCmdException {
            Object openedFile = DefaultLoader.getUIHelper().getOpenedFile(getProject(),
                    storageAccount != null
                        ? storageAccount.name()
                        : clientStorageAccount.getName(),
                    blobContainer);

            if (openedFile != null) {
                DefaultLoader.getIdeHelper().closeFile(getProject(), openedFile);
            }

            try {
                if (storageAccount != null) {
                    StorageClientSDKManager.getManager().deleteBlobContainer(storageAccount, blobContainer);
                } else {
                    StorageClientSDKManager.getManager().deleteBlobContainer(clientStorageAccount, blobContainer);
                }
                parent.removeAllChildNodes();
                ((RefreshableNode) parent).load(false);
            } catch (AzureCmdException ex) {
                throw new RuntimeException("An error occurred while attempting to delete blob storage", ex);
            }
        }

        @Override
        protected String getServiceName(NodeActionEvent event) {
            return STORAGE;
        }

        @Override
        protected String getOperationName(NodeActionEvent event) {
            return DELETE_BLOB_CONTAINER;
        }

        @Override
        protected void onSubscriptionsChanged(NodeActionEvent e)
                throws AzureCmdException {
        }

        @Override
        protected @Nullable String getIconPath() {
            return CommonIcons.ACTION_DISCARD;
        }
    }

    private static final String CONTAINER_MODULE_ID = ContainerNode.class.getName();
    private static final String ICON_PATH = "BlobFile.svg";
    private final BlobContainer blobContainer;
    private StorageAccount storageAccount;
    private ClientStorageAccount clientStorageAccount;

    public ContainerNode(final Node parent, StorageAccount sa, BlobContainer bc) {
        super(CONTAINER_MODULE_ID, bc.getName(), parent, ICON_PATH, true);

        blobContainer = bc;
        storageAccount = sa;

        loadActions();
    }

    public ContainerNode(final Node parent, ClientStorageAccount sa, BlobContainer bc) {
        super(CONTAINER_MODULE_ID, bc.getName(), parent, ICON_PATH, true);

        blobContainer = bc;
        clientStorageAccount = sa;

        loadActions();
    }

    @Override
    protected void onNodeClick(NodeActionEvent e) {
        final Object openedFile = DefaultLoader.getUIHelper().getOpenedFile(getProject(),
                storageAccount != null
                        ? storageAccount.name()
                        : clientStorageAccount.getName()
                , blobContainer);

        if (openedFile == null) {
            if (storageAccount != null) {
                DefaultLoader.getUIHelper().openItem(getProject(), storageAccount, blobContainer, " [Container]", "BlobContainer", "BlobFile.svg");
            } else {
                DefaultLoader.getUIHelper().openItem(getProject(), clientStorageAccount, blobContainer, " [Container]", "BlobContainer", "BlobFile.svg");
            }
        } else {
            DefaultLoader.getUIHelper().openItem(getProject(), openedFile);
        }
    }

    @Override
    protected void refreshItems() throws AzureCmdException {
        String accountName = storageAccount != null ? storageAccount.name() : clientStorageAccount.getName();
        DefaultLoader.getUIHelper().refreshBlobs(getProject(), accountName, blobContainer);
    }

    @Override
    protected void loadActions() {
        addAction(ACTION_VIEW_BLOB_CONTAINER, new ViewBlobContainer());
        addAction(ACTION_DELETE, CommonIcons.ACTION_DISCARD, new DeleteBlobContainer(), NodeActionPosition.BOTTOM);
        super.loadActions();
    }
}
