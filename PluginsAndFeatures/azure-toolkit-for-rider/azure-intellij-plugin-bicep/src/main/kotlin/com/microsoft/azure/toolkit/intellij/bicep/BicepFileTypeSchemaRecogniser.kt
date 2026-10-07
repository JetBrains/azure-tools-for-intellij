/*
 * Copyright 2018-2025 JetBrains s.r.o. and contributors. Use of this source code is governed by the MIT license.
 */

@file:Suppress("UnstableApiUsage")

package com.microsoft.azure.toolkit.intellij.bicep

import com.intellij.internal.statistic.collectors.fus.fileTypes.FileTypeUsageSchemaDescriptor
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile

internal class BicepFileTypeSchemaRecogniser : FileTypeUsageSchemaDescriptor {
  override fun describes(project: Project, file: VirtualFile): Boolean {
    return file.extension == "bicep"
  }
}