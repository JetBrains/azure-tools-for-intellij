package com.microsoft.intellij.serviceexplorer.azure

import com.intellij.openapi.project.Project
import com.microsoft.azure.toolkit.intellij.explorer.AzureExplorer
import com.microsoft.tooling.msservices.serviceexplorer.azure.AzureModule

class AzureModuleImpl(project: Project) : AzureModule(project) {
    override fun loadActions() {
        super.loadActions()
        addAction(SignInOutAction(this))
    }

    override fun refreshFromAzure() {
        super.refreshFromAzure()
        AzureExplorer.refreshAll();
    }
}