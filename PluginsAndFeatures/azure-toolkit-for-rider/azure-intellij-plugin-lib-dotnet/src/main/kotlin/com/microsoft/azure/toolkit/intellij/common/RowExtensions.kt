package com.microsoft.azure.toolkit.intellij.common

import com.intellij.openapi.project.Project
import com.intellij.ui.dsl.builder.Cell
import com.intellij.ui.dsl.builder.Row

fun Row.dotnetProjectComboBox(project: Project): Cell<AzureDotnetProjectComboBox> {
    val component = AzureDotnetProjectComboBox(project) { _ -> true }
    return cell(component)
}
