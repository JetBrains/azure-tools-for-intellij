package com.microsoft.azure.toolkit.intellij.devops.auth.accounts

import com.intellij.collaboration.auth.AccountManager
import com.intellij.collaboration.auth.PersistentDefaultAccountHolder
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.openapi.components.StoragePathMacros
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.platform.util.coroutines.childScope
import kotlinx.coroutines.CoroutineScope

@Service(Service.Level.PROJECT)
@State(name = "AzureDevOpsDefaultAccount", storages = [Storage(StoragePathMacros.WORKSPACE_FILE)])
internal class AzureDevOpsProjectDefaultAccountHolder(project: Project, parentCs: CoroutineScope) :
    PersistentDefaultAccountHolder<AzureDevOpsAccount>(project, parentCs.childScope("Azure DevOps default account scope")) {
    override fun accountManager() = service<AzureDevOpsAccountManager>()

    // Implement later with logging subsystem
    override fun notifyDefaultAccountMissing() {
    }
}
