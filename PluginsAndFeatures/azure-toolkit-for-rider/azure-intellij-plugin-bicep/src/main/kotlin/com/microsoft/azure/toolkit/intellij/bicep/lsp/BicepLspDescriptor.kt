/*
 * Copyright 2018-2025 JetBrains s.r.o. and contributors. Use of this source code is governed by the MIT license.
 */

@file:Suppress("UnstableApiUsage")

package com.microsoft.azure.toolkit.intellij.bicep.lsp

import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.openapi.progress.runBlockingMaybeCancellable
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.platform.lsp.api.ProjectWideLspServerDescriptor
import com.intellij.platform.lsp.api.customization.*
import com.jetbrains.rider.environment.initializeAndGetEnvironment
import com.microsoft.azure.toolkit.intellij.bicep.BicepBundle

internal class BicepLspDescriptor : ProjectWideLspServerDescriptor {
    constructor(project: Project) : super(project, "Bicep LSP") {
        this.lspCustomization = object : LspCustomization() {
            override val inlayHintCustomizer: LspInlayHintCustomizer = LspInlayHintDisabled
            override val formattingCustomizer: LspFormattingCustomizer = object : LspFormattingSupport() {
                override fun shouldFormatThisFileExclusivelyByServer(
                    file: VirtualFile,
                    ideCanFormatThisFileItself: Boolean,
                    serverExplicitlyWantsToFormatThisFile: Boolean
                ): Boolean {
                    return file.extension == BicepBundle.BICEP_EXTENSION
                }
            }
        }
    }

    override fun isSupportedFile(file: VirtualFile): Boolean {
        return file.extension == BicepBundle.BICEP_EXTENSION
    }

    override fun createCommandLine(): GeneralCommandLine {
        return runBlockingMaybeCancellable {
            prepareBicepServerLaunchCommandLine(project.initializeAndGetEnvironment())
        }
    }

    override val lspCustomization: LspCustomization
}