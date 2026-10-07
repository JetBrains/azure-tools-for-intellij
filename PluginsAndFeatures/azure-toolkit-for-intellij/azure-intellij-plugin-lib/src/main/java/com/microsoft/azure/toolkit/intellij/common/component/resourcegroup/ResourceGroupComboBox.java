/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.common.component.resourcegroup;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.keymap.KeymapUtil;
import com.intellij.ui.components.fields.ExtendableTextComponent.Extension;
import com.microsoft.azure.toolkit.intellij.common.AzureComboBox;
import com.microsoft.azure.toolkit.lib.Azure;
import com.microsoft.azure.toolkit.lib.common.action.Action;
import com.microsoft.azure.toolkit.lib.common.cache.CacheManager;
import com.microsoft.azure.toolkit.lib.common.messager.AzureMessageBundle;
import com.microsoft.azure.toolkit.lib.common.model.AbstractAzResource;
import com.microsoft.azure.toolkit.lib.common.model.Subscription;
import com.microsoft.azure.toolkit.lib.common.operation.AzureOperation;
import com.microsoft.azure.toolkit.lib.resource.AzureResources;
import com.microsoft.azure.toolkit.lib.resource.ResourceGroup;
import com.microsoft.azure.toolkit.lib.resource.ResourceGroupDraft;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.swing.*;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

public class ResourceGroupComboBox extends AzureComboBox<ResourceGroup> {
    private Subscription subscription;
    private final List<ResourceGroup> draftItems = new LinkedList<>();

    @Override
    public String getLabel() {
        return "Resource Group";
    }

    @Override
    protected String getItemText(final Object item) {
        if (Objects.isNull(item)) {
            return EMPTY_ITEM;
        }

        final ResourceGroup entity = (ResourceGroup) item;
        if (entity.isDraftForCreating()) {
            return "(New) " + entity.getName();
        }
        return entity.getName();
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
    public void setValue(@Nullable ResourceGroup val, Boolean fixed) {
        if (Objects.nonNull(val) && val.isDraftForCreating() && !val.exists()) {
            this.draftItems.remove(val);
            this.draftItems.add(0, val);
            this.reloadItems();
        }
        super.setValue(val, fixed);
    }

    @Nullable
    @Override
    protected ResourceGroup doGetDefaultValue() {
        return CacheManager.getUsageHistory(ResourceGroup.class)
            .peek(g -> Objects.isNull(subscription) || Objects.equals(subscription.getId(), g.getSubscriptionId()));
    }

    @Nonnull
    @Override
    @AzureOperation(name = "internal/arm.list_resource_groups.subscription", params = {"this.subscription.getId()"})
    protected List<? extends ResourceGroup> loadItems() {
        final List<ResourceGroup> groups = new ArrayList<>();
        if (Objects.nonNull(this.subscription)) {
            final String sid = subscription.getId();
            final List<ResourceGroup> remoteGroups = Azure.az(AzureResources.class).groups(sid).list().stream()
                .sorted(Comparator.comparing(ResourceGroup::getName)).collect(Collectors.toList());
            groups.addAll(remoteGroups);
            if (CollectionUtils.isNotEmpty(this.draftItems)) {
                this.draftItems.stream()
                        .filter(i -> StringUtils.equalsIgnoreCase(this.subscription.getId(), i.getSubscriptionId()))
                        .filter(i -> !remoteGroups.contains(i)) // filter out the draft item which has been created
                        .forEach(groups::add);
            }
        }
        return groups;
    }

    @Override
    protected void refreshItems() {
        Optional.ofNullable(this.subscription).ifPresent(s -> Azure.az(AzureResources.class).groups(s.getId()).refresh());
        super.refreshItems();
    }

    @Nonnull
    @Override
    protected List<Extension> getExtensions() {
        final List<Extension> extensions = super.getExtensions();
        final KeyStroke keyStroke = KeyStroke.getKeyStroke(KeyEvent.VK_INSERT, InputEvent.ALT_DOWN_MASK);
        final String tooltip = String.format("%s (%s)", AzureMessageBundle.message("common.resourceGroup.create.tooltip").toString(), KeymapUtil.getKeystrokeText(keyStroke));
        final Extension addEx = Extension.create(AllIcons.General.Add, tooltip, this::showResourceGroupCreationPopup);
        this.registerShortcut(keyStroke, addEx);
        extensions.add(addEx);
        return extensions;
    }

    private void showResourceGroupCreationPopup() {
        final ResourceGroupCreationDialog dialog = new ResourceGroupCreationDialog(this.subscription);
        final Action.Id<ResourceGroupDraft> actionId = Action.Id.of("user/arm.create_group.rg");
        dialog.setOkAction(new Action<>(actionId)
            .withLabel("Create")
            .withIdParam(AbstractAzResource::getName)
            .withSource(s -> s)
            .withAuthRequired(false)
            .withHandler(draft -> this.setValue(draft)));
        dialog.show();
    }
}
