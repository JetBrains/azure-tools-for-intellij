/*
 * Copyright 2018-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the MIT license.
 */

package com.microsoft.azure.toolkit.intellij.debugger

import com.intellij.execution.configurations.RunProfile
import com.microsoft.azure.toolkit.intellij.debugger.attachHosts.AppServiceAttachHost
import javax.swing.Icon

class AzureAppServiceAttachProfile(private val host: AppServiceAttachHost<*>, innerProfile: RunProfile) :
    RunProfile by innerProfile {

    override fun getName(): String = host.appServiceApp.name
    override fun getIcon(): Icon? = host.appServiceApp.getIcon()
}