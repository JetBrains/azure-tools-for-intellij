package com.microsoft.azure.toolkit.intellij.devops.util

import com.intellij.collaboration.async.PluginScopeProviderBase
import com.intellij.openapi.components.Service
import kotlinx.coroutines.CoroutineScope

@Suppress("UnstableApiUsage")
@Service(Service.Level.PROJECT)
class AzureDevOpsPluginProjectScopeProvider(parentCs: CoroutineScope) : PluginScopeProviderBase(parentCs) {
}