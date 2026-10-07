/*
 * Copyright 2018-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the MIT license.
 */

package com.microsoft.azure.toolkit.intellij.storage.connection

import com.intellij.openapi.project.Project
import com.microsoft.azure.toolkit.intellij.common.AzureFormJPanel
import com.microsoft.azure.toolkit.intellij.connector.Resource
import com.microsoft.azure.toolkit.lib.storage.IStorageAccount

class StorageAccountResourceDefinition : BaseStorageAccountResourceDefinition() {
    companion object {
        val INSTANCE = StorageAccountResourceDefinition()
    }

    override fun getResourcePanel(project: Project?): AzureFormJPanel<Resource<IStorageAccount>> {
        val panel = StorageAccountResourcePanel()
        return panel
    }
}