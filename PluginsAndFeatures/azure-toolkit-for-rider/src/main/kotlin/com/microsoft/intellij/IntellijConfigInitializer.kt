package com.microsoft.intellij

import com.intellij.openapi.application.PermanentInstallationID
import com.microsoft.azure.toolkit.ide.common.store.AzureConfigInitializer
import com.microsoft.azure.toolkit.lib.Azure
import com.microsoft.azure.toolkit.lib.auth.AzureCloud
import com.microsoft.azure.toolkit.lib.common.utils.InstallationIdUtils
import com.microsoft.azuretools.authmanage.CommonSettings

object IntellijConfigInitializer {
    fun initialize() {
        var installId = InstallationIdUtils.getHashMac()
        if (installId.isNullOrBlank() || !InstallationIdUtils.isValidHashMac(installId)) {
            installId = InstallationIdUtils.hash(PermanentInstallationID.get())
        }

        AzureConfigInitializer.initialize(installId, "Azure Toolkit for IntelliJ", AzurePlugin.PLUGIN_VERSION)
        CommonSettings.setUserAgent(Azure.az().config().userAgent)
        val cloud = Azure.az().config().cloud
        if (cloud.isNotBlank()) {
            Azure.az(AzureCloud::class.java).setByName(cloud)
        }
    }
}