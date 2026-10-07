/*
 * Copyright 2018-2025 JetBrains s.r.o. and contributors. Use of this source code is governed by the MIT license.
 */

package com.microsoft.azure.toolkit.intellij.bicep.settings

import com.intellij.openapi.components.BaseState

internal class BicepSettingsState : BaseState() {
  var ignoreLsDownloadSuggestion: Boolean by property(false)
}