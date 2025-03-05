/*
 * Copyright 2018-2025 JetBrains s.r.o. and contributors. Use of this source code is governed by the MIT license.
 */

package com.microsoft.azure.toolkit.intellij.aspire

import com.intellij.util.io.systemIndependentPath
import com.jetbrains.rider.model.RunnableProject
import com.jetbrains.rider.model.RunnableProjectsModel
import com.microsoft.azure.toolkit.intellij.legacy.function.daemon.AzureRunnableProjectKinds
import java.nio.file.Path

fun RunnableProjectsModel.findBySessionProject(sessionProjectPath: Path): RunnableProject? {
    val runnableProjects = projects.valueOrNull ?: return null
    val sessionProjectPathString = sessionProjectPath.systemIndependentPath
    return runnableProjects.singleOrNull {
        it.projectFilePath == sessionProjectPathString && it.kind == AzureRunnableProjectKinds.AzureFunctions
    }
}