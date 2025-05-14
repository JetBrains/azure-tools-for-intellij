package com.microsoft.azure.toolkit.intellij.devops.api

import com.intellij.openapi.components.Service

@Service(Service.Level.APP)
class AzureDevOpsApiManager {

    fun getClient(
        server: AzureDevOpsServerPath,
        token: String
    ): AzureDevOpsApi =
        getClient(server) { token }

    fun getClient(server: AzureDevOpsServerPath, tokenSupplier: () -> String): AzureDevOpsApi =
        AzureDevOpsApiImpl(server, tokenSupplier)
}
