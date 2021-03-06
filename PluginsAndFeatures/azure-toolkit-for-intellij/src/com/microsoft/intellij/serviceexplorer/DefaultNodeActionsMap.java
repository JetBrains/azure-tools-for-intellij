/*
 * Copyright (c) Microsoft Corporation
 * Copyright (c) 2018-2021 JetBrains s.r.o.
 *
 * All rights reserved.
 *
 * MIT License
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation files (the "Software"), to deal in the Software without restriction, including without limitation
 * the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and
 * to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of
 * the Software.
 *
 * THE SOFTWARE IS PROVIDED *AS IS*, WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO
 * THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT,
 * TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.microsoft.intellij.serviceexplorer;

import com.google.common.collect.ImmutableList;
import com.microsoft.azure.toolkit.intellij.appservice.action.StartStreamingLogsAction;
import com.microsoft.azure.toolkit.intellij.appservice.action.StopStreamingLogsAction;
import com.microsoft.azure.toolkit.intellij.arm.action.*;
import com.microsoft.azure.toolkit.intellij.mysql.action.CreateMySQLAction;
import com.microsoft.azure.toolkit.intellij.mysql.action.MySQLConnectToServerAction;
import com.microsoft.azure.toolkit.intellij.redis.action.CreateRedisCacheAction;
import com.microsoft.azure.toolkit.intellij.vm.CreateVMAction;
import com.microsoft.intellij.serviceexplorer.azure.storage.*;
import com.microsoft.intellij.serviceexplorer.azure.storagearm.CreateStorageAccountAction;
import com.microsoft.tooling.msservices.serviceexplorer.Node;
import com.microsoft.tooling.msservices.serviceexplorer.NodeActionListener;
import com.microsoft.tooling.msservices.serviceexplorer.azure.arm.ResourceManagementModule;
import com.microsoft.tooling.msservices.serviceexplorer.azure.arm.ResourceManagementNode;
import com.microsoft.tooling.msservices.serviceexplorer.azure.arm.deployments.DeploymentNode;
import com.microsoft.tooling.msservices.serviceexplorer.azure.function.deploymentslot.FunctionDeploymentSlotNode;
import com.microsoft.tooling.msservices.serviceexplorer.azure.mysql.MySQLModule;
import com.microsoft.tooling.msservices.serviceexplorer.azure.mysql.MySQLNode;
import com.microsoft.tooling.msservices.serviceexplorer.azure.rediscache.RedisCacheModule;
import com.microsoft.tooling.msservices.serviceexplorer.azure.storage.*;
import com.microsoft.tooling.msservices.serviceexplorer.azure.storage.asm.ClientBlobModule;
import com.microsoft.tooling.msservices.serviceexplorer.azure.vmarm.VMArmModule;
import com.microsoft.tooling.msservices.serviceexplorer.azure.webapp.deploymentslot.DeploymentSlotNode;

import java.util.*;

public class DefaultNodeActionsMap extends NodeActionsMap {

    private static final Map<Class<? extends Node>, ImmutableList<Class<? extends NodeActionListener>>> node2Actions =
            new HashMap<>();

    static {
        node2Actions.put(VMArmModule.class, new ImmutableList.Builder<Class<? extends NodeActionListener>>()
            .add(CreateVMAction.class).build());

        node2Actions.put(QueueModule.class, new ImmutableList.Builder<Class<? extends NodeActionListener>>()
            .add(CreateQueueAction.class).build());

        node2Actions.put(TableModule.class, new ImmutableList.Builder<Class<? extends NodeActionListener>>()
            .add(CreateTableAction.class).build());

        node2Actions.put(BlobModule.class, new ImmutableList.Builder<Class<? extends NodeActionListener>>()
            .add(CreateBlobContainer.class).build());

        node2Actions.put(StorageModule.class, new ImmutableList.Builder<Class<? extends NodeActionListener>>()
            .add(CreateStorageAccountAction.class)
            .add(AttachExternalStorageAccountAction.class).build());

        node2Actions.put(ClientBlobModule.class, new ImmutableList.Builder<Class<? extends NodeActionListener>>()
            .add(CreateBlobContainer.class).build());

        node2Actions.put(StorageNode.class, new ImmutableList.Builder<Class<? extends NodeActionListener>>()
            .add(CreateBlobContainer.class).build());

        node2Actions.put(RedisCacheModule.class, new ImmutableList.Builder<Class<? extends NodeActionListener>>()
            .add(CreateRedisCacheAction.class).build());

        node2Actions.put(MySQLModule.class, new ImmutableList.Builder<Class<? extends NodeActionListener>>()
            .add(CreateMySQLAction.class).build());

        node2Actions.put(MySQLNode.class, new ImmutableList.Builder<Class<? extends NodeActionListener>>()
            .add(MySQLConnectToServerAction.class)
            .build());

        // todo: what is ConfirmDialogAction?
        //noinspection unchecked
        node2Actions.put(ExternalStorageNode.class, new ImmutableList.Builder<Class<? extends NodeActionListener>>()
            .add(ConfirmDialogAction.class, ModifyExternalStorageAccountAction.class).build());

        final List<Class<? extends NodeActionListener>> deploymentNodeList = new ArrayList<>(Arrays.asList(
                EditDeploymentAction.class,
                UpdateDeploymentAction.class,
                ExportTemplateAction.class,
                ExportParameterAction.class));

        node2Actions.put(DeploymentNode.class, new ImmutableList.Builder<Class<? extends NodeActionListener>>()
            .addAll(deploymentNodeList).build());

        node2Actions.put(ResourceManagementModule.class, new ImmutableList.Builder<Class<? extends NodeActionListener>>()
            .add(CreateDeploymentAction.class).build());

        node2Actions.put(ResourceManagementNode.class, new ImmutableList.Builder<Class<? extends NodeActionListener>>()
            .add(CreateDeploymentAction.class).build());

        node2Actions.put(DeploymentSlotNode.class, new ImmutableList.Builder<Class<? extends NodeActionListener>>()
            .add(StartStreamingLogsAction.class)
            .add(StopStreamingLogsAction.class).build());

        node2Actions.put(FunctionDeploymentSlotNode.class, new ImmutableList.Builder<Class<? extends NodeActionListener>>()
            .add(StartStreamingLogsAction.class)
            .add(StopStreamingLogsAction.class).build());
    }

    @Override
    public Map<Class<? extends Node>, ImmutableList<Class<? extends NodeActionListener>>> getMap() {
        return node2Actions;
    }
}
