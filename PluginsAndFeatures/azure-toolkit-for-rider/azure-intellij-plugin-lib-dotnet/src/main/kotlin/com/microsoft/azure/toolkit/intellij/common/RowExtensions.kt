/*
 * Copyright 2018-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the MIT license.
 */

package com.microsoft.azure.toolkit.intellij.common

import com.intellij.openapi.project.Project
import com.intellij.ui.dsl.builder.Cell
import com.intellij.ui.dsl.builder.Row

fun Row.dockerContainerRegistryComboBox(project: Project): Cell<AzureContainerRegistryComboBox> {
    val comboBox = AzureContainerRegistryComboBox(project)
    return cell(comboBox)
}