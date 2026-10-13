/*
 * Copyright 2018-2025 JetBrains s.r.o. and contributors. Use of this source code is governed by the MIT license.
 */

package com.microsoft.azure.toolkit.intellij.bicep.settings

import com.intellij.openapi.components.BaseState

internal class BicepSettingsState : BaseState() {
  var ignoreLsDownloadSuggestion: Boolean by property(false)
  var languageServerSource: LanguageServerSource by enum(LanguageServerSource.LATEST)
  var languageServerVersion: String? by string()
  var languageServerPath: String? by string()

  internal enum class LanguageServerSource {
    LATEST,
    SPECIFIC_VERSION,
    LOCAL_FILE
  }
}
