package com.microsoft.azure.toolkit.intellij.bicep.settings

import com.intellij.openapi.components.*
import com.intellij.openapi.project.Project
import com.microsoft.azure.toolkit.intellij.bicep.settings.BicepSettingsState.LanguageServerSource

@Service(Service.Level.PROJECT)
@State(name = "AzureBicepSettings", storages = [Storage(StoragePathMacros.WORKSPACE_FILE)])
internal class BicepSettings : SimplePersistentStateComponent<BicepSettingsState>(BicepSettingsState()) {
  companion object {
    fun getInstance(project: Project) = project.service<BicepSettings>()
  }

  fun rememberToIgnoreLsDownloadSuggestion() {
    state.ignoreLsDownloadSuggestion = true
  }

  fun shouldIgnoreLsDownloadSuggestion(): Boolean {
    return state.ignoreLsDownloadSuggestion
  }

  fun getLanguageServerSource(): LanguageServerSource {
    return state.languageServerSource
  }

  fun getLanguageServerVersion(): String? {
    return state.languageServerVersion
  }

  fun getLanguageServerPath(): String? {
    return state.languageServerPath
  }
}
