/*
 * Copyright 2018-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the MIT license.
 */

package com.microsoft.azure.toolkit.intellij

import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import kotlinx.coroutines.CoroutineScope

@Service(Service.Level.PROJECT)
class AppServiceProjectService(val scope: CoroutineScope) {
    companion object {
        fun getInstance(project: Project) = project.service<AppServiceProjectService>()
    }
}