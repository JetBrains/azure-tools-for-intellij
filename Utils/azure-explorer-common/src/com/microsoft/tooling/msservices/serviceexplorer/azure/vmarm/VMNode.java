/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Copyright (c) 2018-2021 JetBrains s.r.o.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.tooling.msservices.serviceexplorer.azure.vmarm;

import com.microsoft.azure.CloudException;
import com.microsoft.azure.management.compute.InstanceViewStatus;
import com.microsoft.azure.management.compute.VirtualMachine;
import com.microsoft.azure.management.resources.fluentcore.arm.ResourceId;
import com.microsoft.azure.toolkit.lib.common.operation.AzureOperation;
import com.microsoft.azuretools.ActionConstants;
import com.microsoft.azuretools.authmanage.AuthMethodManager;
import com.microsoft.azuretools.azurecommons.helpers.AzureCmdException;
import com.microsoft.azuretools.azurecommons.helpers.Nullable;
import com.microsoft.azuretools.sdkmanage.AzureManager;
import com.microsoft.azuretools.telemetry.AppInsightsConstants;
import com.microsoft.azuretools.telemetry.TelemetryProperties;
import com.microsoft.tooling.msservices.components.DefaultLoader;
import com.microsoft.tooling.msservices.serviceexplorer.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class VMNode extends RefreshableNode implements TelemetryProperties {
    private static String RUNNING_STATUS = "PowerState/running";
    private static String STOPPED = "stopped";

    private NodeAction startAction;
    private NodeAction restartAction;
    private NodeAction stopAction;

    @Override
    public Map<String, String> toProperties() {
        final Map<String, String> properties = new HashMap<>();
        properties.put(AppInsightsConstants.SubscriptionId, ResourceId.fromString(this.virtualMachine.id()).subscriptionId());
        properties.put(AppInsightsConstants.Region, this.virtualMachine.regionName());
        return properties;
    }

    @Override
    public @Nullable AzureIconSymbol getIconSymbol() {
        boolean running = isRunning();
        return running ? AzureIconSymbol.VirtualMachine.RUNNING : AzureIconSymbol.VirtualMachine.STOPPED;
    }

    @AzureOperation(name = ActionConstants.VirtualMachine.DELETE, type = AzureOperation.Type.ACTION)
    private void delete() {
        AzureManager azureManager = AuthMethodManager.getInstance().getAzureManager();
        // not signed in
        if (azureManager == null) {
            return;
        }
        azureManager.getAzure(subscriptionId).virtualMachines().deleteByResourceGroup(virtualMachine.resourceGroupName(), virtualMachine.name());
        DefaultLoader.getIdeHelper().invokeLater(() -> {
            // instruct parent node to remove this node
            getParent().removeDirectChildNode(VMNode.this);
        });
    }

    @AzureOperation(name = ActionConstants.VirtualMachine.START, type = AzureOperation.Type.ACTION)
    private void start() {
        virtualMachine.start();
        refreshItems();
    }

    @AzureOperation(name = ActionConstants.VirtualMachine.RESTART, type = AzureOperation.Type.ACTION)
    private void restart() {
        virtualMachine.restart();
        refreshItems();
    }

    @AzureOperation(name = ActionConstants.VirtualMachine.STOP, type = AzureOperation.Type.ACTION)
    private void stop() {
        virtualMachine.powerOff();
        refreshItems();
    }

    private static final String WAIT_ICON_PATH = "VirtualMachineUpdating_16.png";
    private static final String STOP_ICON_PATH = "VirtualMachineStopped_16.png";
    private static final String RUN_ICON_PATH = "VirtualMachineRunning_16.png";

    private VirtualMachine virtualMachine;
    private String subscriptionId;

    public VMNode(Node parent, String subscriptionId, VirtualMachine virtualMachine)
            throws AzureCmdException {
        super(virtualMachine.id(), virtualMachine.name(), parent, WAIT_ICON_PATH, true);
        this.virtualMachine = virtualMachine;
        this.subscriptionId = subscriptionId;
        loadActions();

        // update vm icon based on vm status
        refreshItemsInternal();
    }

    private String getVMIconPath() {
        try {
            for (InstanceViewStatus status : virtualMachine.instanceView().statuses()) {
                if (RUNNING_STATUS.equalsIgnoreCase(status.code())) {
                    return RUN_ICON_PATH;
                }
                if (status.code().toLowerCase().contains(STOPPED)) {
                    return STOP_ICON_PATH;
                }
            }
        } catch (CloudException e) {
            DefaultLoader.getUIHelper().logError(e.getMessage(), e);
        }
        return WAIT_ICON_PATH;
    }

    @Override
    protected void refreshItems() {
        virtualMachine.refresh();

        refreshItemsInternal();
    }

    private void refreshItemsInternal() {
        // update vm name and status icon
        setName(virtualMachine.name());
        setIconPath(getVMIconPath());
    }

    @Override
    protected void loadActions() {
        addAction(initActionBuilder(this::start).withAction(AzureActionEnum.START).withBackgroudable(true).withPromptable(true).build());
        addAction(initActionBuilder(this::restart).withAction(AzureActionEnum.RESTART).withBackgroudable(true).build());
        addAction(initActionBuilder(this::stop).withAction(AzureActionEnum.STOP).withBackgroudable(true).withPromptable(true).build());
        addAction(initActionBuilder(this::delete).withAction(AzureActionEnum.DELETE).withBackgroudable(true).withPromptable(true).build());
        super.loadActions();
    }

    protected final BasicActionBuilder initActionBuilder(Runnable runnable) {
        return new BasicActionBuilder(runnable)
                .withModuleName(VMArmModule.MODULE_NAME)
                .withInstanceName(name);
    }

    @Override
    public List<NodeAction> getNodeActions() {
        if (startAction == null)
            startAction = getNodeActionByName(AzureActionEnum.START.getName());

        if (restartAction == null)
            restartAction = getNodeActionByName(AzureActionEnum.RESTART.getName());

        if (stopAction == null)
            stopAction = getNodeActionByName(AzureActionEnum.STOP.getName());

        List<NodeAction> nodeActions = super.getNodeActions();

//        // enable/disable menu items according to VM status
        boolean started = isRunning();
        if (stopAction != null)
            stopAction.setEnabled(started);

        if (startAction != null)
            startAction.setEnabled(!started);

        if (restartAction != null)
            restartAction.setEnabled(started);

        if (started) {
            int startIndex = nodeActions.indexOf(startAction);
            nodeActions.remove(startAction);
            if (!nodeActions.contains(stopAction))
                nodeActions.add(startIndex, stopAction);
        } else {
            int stopIndex = nodeActions.indexOf(stopAction);
            nodeActions.remove(stopAction);
            if (!nodeActions.contains(startAction))
                nodeActions.add(stopIndex, startAction);
        }

        restartAction.setEnabled(started);

        return nodeActions;
    }

    private boolean isRunning() {
        try {
            for (InstanceViewStatus status : virtualMachine.instanceView().statuses()) {
                if (RUNNING_STATUS.equalsIgnoreCase(status.code())) {
                    return true;
                }
                if (status.code().toLowerCase().contains(STOPPED)) {
                    return false;
                }
            }
        } catch (CloudException e) {
            DefaultLoader.getUIHelper().logError(e.getMessage(), e);
        }
        return false;
    }
}
