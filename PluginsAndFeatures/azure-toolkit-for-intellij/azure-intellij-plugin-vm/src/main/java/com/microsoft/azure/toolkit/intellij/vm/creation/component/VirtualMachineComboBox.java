/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.vm.creation.component;

import com.microsoft.azure.toolkit.intellij.common.AzureComboBox;
import com.microsoft.azure.toolkit.intellij.vm.creation.VMCreationDialog;
import com.microsoft.azure.toolkit.lib.Azure;
import com.microsoft.azure.toolkit.lib.common.action.Action;
import com.microsoft.azure.toolkit.lib.common.cache.CacheManager;
import com.microsoft.azure.toolkit.lib.common.model.AbstractAzResource;
import com.microsoft.azure.toolkit.lib.common.model.Subscription;
import com.microsoft.azure.toolkit.lib.compute.AzureCompute;
import com.microsoft.azure.toolkit.lib.compute.virtualmachine.VirtualMachine;
import com.microsoft.azure.toolkit.lib.compute.virtualmachine.VirtualMachineDraft;
import org.apache.commons.collections4.CollectionUtils;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

public class VirtualMachineComboBox extends AzureComboBox<VirtualMachine> {
    private final List<VirtualMachine> draftItems = new LinkedList<>();
    private Subscription subscription;

    @Override
    protected String getItemText(Object item) {
        return item instanceof VirtualMachine ? ((VirtualMachine) item).getName() : super.getItemText(item);
    }

    public void setSubscription(Subscription subscription) {
        if (Objects.equals(subscription, this.subscription)) {
            return;
        }
        this.subscription = subscription;
        if (subscription == null) {
            this.clear();
            return;
        }
        this.reloadItems();
    }

    @Override
    public void setValue(@Nullable VirtualMachine val, Boolean fixed) {
        if (Objects.nonNull(val) && val.isDraftForCreating()) {
            this.draftItems.remove(val);
            this.draftItems.add(0, val);
            this.reloadItems();
        }
        super.setValue(val, fixed);
    }

    @Nullable
    @Override
    protected VirtualMachine doGetDefaultValue() {
        return CacheManager.getUsageHistory(VirtualMachine.class)
            .peek(v -> Objects.isNull(subscription) || Objects.equals(subscription.getId(), v.getSubscriptionId()));
    }

//    @Nullable
//    @Override
//    protected ExtendableTextComponent.Extension getExtension() {
//        return ExtendableTextComponent.Extension.create(
//            AllIcons.General.Add, AzureMessageBundle.message("vm.create.tooltip").toString(), this::showVirtualMachineCreationPopup);
//    }

    @Nonnull
    @Override
    protected List<? extends VirtualMachine> loadItems() {
        final List<VirtualMachine> vms = new ArrayList<>();
        if (CollectionUtils.isNotEmpty(this.draftItems)) {
            vms.addAll(new ArrayList<>(this.draftItems));
        }
        if (Objects.nonNull(subscription)) {
            final List<VirtualMachine> remoteVms = Azure.az(AzureCompute.class)
                .virtualMachines(subscription.getId()).list().stream()
                .sorted(Comparator.comparing(VirtualMachine::getName)).collect(Collectors.toList());
            remoteVms.parallelStream().forEach(VirtualMachine::getHostIp); // pre-load host ip
            vms.addAll(remoteVms);
        }
        return vms;
    }

    @Override
    protected void refreshItems() {
        Optional.ofNullable(this.subscription).ifPresent(s -> Azure.az(AzureCompute.class).virtualMachines(s.getId()).refresh());
        super.refreshItems();
    }

    private void showVirtualMachineCreationPopup() {
        final VMCreationDialog dialog = new VMCreationDialog(null);
        final Action.Id<VirtualMachineDraft> actionId = Action.Id.of("user/vm.create_vm.vm");
        dialog.setOkAction(new Action<>(actionId)
            .withLabel("Create")
            .withIdParam(AbstractAzResource::getName)
            .withSource(s -> s)
            .withAuthRequired(false)
            .withHandler(vm -> this.setValue(vm)));
        dialog.show();
    }
}
