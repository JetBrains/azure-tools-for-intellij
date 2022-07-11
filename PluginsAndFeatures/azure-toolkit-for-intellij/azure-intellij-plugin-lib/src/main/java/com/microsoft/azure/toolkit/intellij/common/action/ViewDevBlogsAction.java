/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.common.action;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.DumbAware;
import com.microsoft.azure.toolkit.lib.common.action.AzureActionManager;
import org.jetbrains.annotations.NotNull;

import static com.microsoft.azure.toolkit.ide.common.action.ResourceCommonActionsContributor.OPEN_URL;

public class ViewDevBlogsAction extends AnAction implements DumbAware {
    public static final String DEV_BLOGS_URL = "https://aka.ms/javaToolingBlogs";

    @Override
    public void actionPerformed(@NotNull AnActionEvent anActionEvent) {
        AzureActionManager.getInstance().getAction(OPEN_URL).handle(DEV_BLOGS_URL);
    }
}