/*
 * Copyright 2018-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the MIT license.
 */

package com.microsoft.azure.toolkit.intellij

import com.intellij.openapi.util.IconLoader

internal object AppServiceIcons {
    @JvmField
    val ResourceGroup = IconLoader.getIcon("/icons/resource-group.svg", javaClass)
    @JvmField
    val WebApp = IconLoader.getIcon("/icons/web-app.svg", javaClass)
    @JvmField
    val LinuxWebApp = IconLoader.getIcon("/icons/linux-web-app.svg", javaClass)
    @JvmField
    val FunctionApp = IconLoader.getIcon("/icons/function-app.svg", javaClass)
    @JvmField
    val LinuxFunctionApp = IconLoader.getIcon("/icons/linux-function-app.svg", javaClass)
    @JvmField
    val DeploymentSlot = IconLoader.getIcon("/icons/deployment-slot.svg", javaClass)
}