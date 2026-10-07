package com.microsoft.azure.toolkit.intellij.bicep.settings

import com.intellij.openapi.components.*
import com.intellij.openapi.project.Project

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
}

