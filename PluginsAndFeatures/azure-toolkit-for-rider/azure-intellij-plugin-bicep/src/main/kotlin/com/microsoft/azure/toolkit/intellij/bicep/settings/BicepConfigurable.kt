package com.microsoft.azure.toolkit.intellij.bicep.settings

import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory
import com.intellij.openapi.options.BoundConfigurable
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogPanel
import com.intellij.ui.dsl.builder.Align
import com.intellij.ui.dsl.builder.actionButton
import com.intellij.ui.dsl.builder.bind
import com.intellij.ui.dsl.builder.bindText
import com.intellij.ui.dsl.builder.panel
import com.intellij.ui.layout.selected
import com.microsoft.azure.toolkit.intellij.bicep.BicepBundle
import com.microsoft.azure.toolkit.intellij.bicep.settings.BicepSettingsState.LanguageServerSource

internal class BicepConfigurable(private val project: Project) : BoundConfigurable(BicepBundle.message("settings.display.name")) {
  override fun createPanel(): DialogPanel {
    val state = BicepSettings.getInstance(project).state
    return panel {
      group(BicepBundle.message("settings.section.ls.name")) {
        buttonsGroup {
          row {
            radioButton(BicepBundle.message("settings.ls.source.latest"), LanguageServerSource.LATEST)
          }
          row {
            val specificVersion = radioButton(BicepBundle.message("settings.ls.source.version"), LanguageServerSource.SPECIFIC_VERSION)
            textField()
              .bindText({ state.languageServerVersion.orEmpty() }, { state.languageServerVersion = it })
              .enabledIf(specificVersion.component.selected)
              .validationOnApply {
                if (specificVersion.component.isSelected && it.text.isBlank()) {
                  error(BicepBundle.message("settings.ls.version.required"))
                }
                else null
              }
          }
          row {
            val localServer = radioButton(BicepBundle.message("settings.ls.source.local"), LanguageServerSource.LOCAL_FILE)
            @Suppress("DialogTitleCapitalization")
            val descriptor = FileChooserDescriptorFactory.createSingleFileNoJarsDescriptor()
              .withTitle(BicepBundle.message("settings.ls.path.browse.title"))
              .withFileFilter { it.name == "Bicep.LangServer.dll" }
            @Suppress("UnstableApiUsage")
            textFieldWithBrowseButton(descriptor, project)
              .align(Align.FILL)
              .bindText({ state.languageServerPath.orEmpty() }, { state.languageServerPath = it })
              .enabledIf(localServer.component.selected)
              .validationOnApply {
                if (localServer.component.isSelected && it.text.isEmpty()) {
                  error(BicepBundle.message("settings.ls.path.required"))
                }
                else null
              }
          }
        }.bind(state::languageServerSource)
      }
    }
  }
}
