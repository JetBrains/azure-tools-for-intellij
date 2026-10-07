package com.microsoft.azure.toolkit.intellij.bicep.settings

import com.microsoft.azure.toolkit.intellij.bicep.BicepBundle
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.options.Configurable
import com.intellij.ui.dsl.builder.actionButton
import com.intellij.ui.dsl.builder.panel
import javax.swing.JComponent

class BicepConfigurable : Configurable {
  override fun createComponent(): JComponent {
    return panel {
      group(BicepBundle.message("settings.section.ls.name")) {
        row(BicepBundle.message("action.BicepInstallLsInfrastructureAction.text")) {
          actionButton(ActionManager.getInstance().getAction("BicepInstallLsInfrastructureAction"))
        }
      }
    }
  }

  override fun isModified(): Boolean {
    return false
  }

  override fun apply() {}

  override fun getDisplayName(): String {
    return BicepBundle.message("settings.display.name")
  }
}

