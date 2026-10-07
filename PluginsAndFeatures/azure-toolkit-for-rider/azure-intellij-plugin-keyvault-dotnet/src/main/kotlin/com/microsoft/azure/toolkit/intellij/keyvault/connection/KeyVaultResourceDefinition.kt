/*
 * Copyright 2018-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the MIT license.
 */

package com.microsoft.azure.toolkit.intellij.keyvault.connection

import com.intellij.openapi.project.Project

class KeyVaultResourceDefinition : BaseKeyVaultResourceDefinition() {
    companion object {
        val INSTANCE = KeyVaultResourceDefinition()
    }

    override fun getResourcePanel(project: Project?) = KeyVaultResourcePanel()
}