/*
 * Copyright 2018-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the MIT license.
 */

package com.microsoft.azure.toolkit.intellij.storage.azurite.settings

import com.intellij.openapi.components.BaseState

class AzuriteSettingsState : BaseState() {
    var executablePath by string("")
    var locationMode by enum<AzuriteLocationMode>(AzuriteLocationMode.Managed)
    var workspacePath by string("")
    var looseMode by property(false)
    var skipApiVersionCheck by property(false)
    var showAzuriteService by property(true)
    var checkAzuriteExecutable by property(true)
    var blobHost by string("127.0.0.1")
    var blobPort by property(10000)
    var queueHost by string("127.0.0.1")
    var queuePort by property(10001)
    var tableHost by string("127.0.0.1")
    var tablePort by property(10002)
    var basicOAuth by property(false)
    var certificatePath by string("")
    var certificateKeyPath by string("")
    var certificatePassword by string("")
}