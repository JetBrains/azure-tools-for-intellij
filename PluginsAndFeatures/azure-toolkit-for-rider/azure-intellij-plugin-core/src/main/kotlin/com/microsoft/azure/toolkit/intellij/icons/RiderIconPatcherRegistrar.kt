/*
 * Copyright 2018-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the MIT license.
 */

package com.microsoft.azure.toolkit.intellij.icons

import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.ProjectActivity

internal class RiderIconPatcherRegistrar : ProjectActivity {
    override suspend fun execute(project: Project) {
        RiderIconPatcher.install()
    }
}
