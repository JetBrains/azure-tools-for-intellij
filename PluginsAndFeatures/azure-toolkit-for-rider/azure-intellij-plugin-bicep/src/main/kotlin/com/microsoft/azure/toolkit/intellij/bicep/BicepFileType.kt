/*
 * Copyright 2018-2025 JetBrains s.r.o. and contributors. Use of this source code is governed by the MIT license.
 */

package com.microsoft.azure.toolkit.intellij.bicep

import com.intellij.icons.AllIcons
import com.intellij.openapi.fileTypes.FileType
import org.jetbrains.plugins.textmate.TextMateBackedFileType
import javax.swing.Icon

internal class BicepFileType private constructor() : FileType, TextMateBackedFileType {
  override fun getName(): String {
    return "Bicep"
  }

  override fun getDescription(): String {
    return BicepBundle.message("file.type.description")
  }

  override fun getDisplayName(): String {
    return BicepBundle.message("file.type.display.name")
  }

  override fun getDefaultExtension(): String {
    return "bicep"
  }

  override fun getIcon(): Icon {
    return AllIcons.Providers.Azure
  }

  override fun isBinary(): Boolean {
    return false
  }
}