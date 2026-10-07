/*
 * Copyright 2018-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the MIT license.
 */

package com.microsoft.azure.toolkit.intellij.appservice

import com.microsoft.azure.toolkit.intellij.appservice.dotnetRuntime.DotNetRuntime
import com.microsoft.azure.toolkit.intellij.appservice.dotnetRuntime.DotNetRuntimeConfig
import com.microsoft.azure.toolkit.intellij.common.RiderRunProcessHandlerMessager
import com.microsoft.azure.toolkit.lib.appservice.AppServiceAppBase
import com.microsoft.azure.toolkit.lib.appservice.model.DockerConfiguration
import com.microsoft.azure.toolkit.lib.common.operation.OperationContext
import com.microsoft.azure.toolkit.lib.common.task.AzureTask
import java.util.concurrent.Callable

abstract class CreateAppServiceTask<T>(
    private val processHandlerMessager: RiderRunProcessHandlerMessager?
) : AzureTask<T>() where T : AppServiceAppBase<*, *, *> {

    private val subTasks: MutableList<AzureTask<*>> = mutableListOf()

    protected fun <R> registerSubTask(task: AzureTask<R>?, consumer: (result: R) -> Unit) {
        if (task != null) {
            subTasks.add(AzureTask<R>(Callable {
                val result = task.body.call()
                consumer(result)
                return@Callable result
            }))
        }
    }

    protected fun executeSubTasks() {
        processHandlerMessager?.let { OperationContext.current().messager = it }

        for (task in subTasks) {
            task.body.call()
        }
    }

    protected fun getRuntime(runtimeConfig: DotNetRuntimeConfig?): DotNetRuntime? {
        if (runtimeConfig == null) return null

        return if (!runtimeConfig.isDocker) {
            DotNetRuntime(
                runtimeConfig.os(),
                runtimeConfig.stack,
                runtimeConfig.dotnetVersion,
                runtimeConfig.frameworkVersion,
                runtimeConfig.functionStack,
                false
            )
        } else {
            DotNetRuntime(
                runtimeConfig.os(),
                null,
                null,
                null,
                null,
                true
            )
        }
    }

    protected fun getDockerConfiguration(runtimeConfig: DotNetRuntimeConfig?): DockerConfiguration? {
        if (runtimeConfig == null || !runtimeConfig.isDocker) return null

        return DockerConfiguration.builder()
            .userName(runtimeConfig.username())
            .password(runtimeConfig.password())
            .registryUrl(runtimeConfig.registryUrl())
            .image(runtimeConfig.image())
            .startUpCommand(runtimeConfig.startUpCommand())
            .build()
    }
}