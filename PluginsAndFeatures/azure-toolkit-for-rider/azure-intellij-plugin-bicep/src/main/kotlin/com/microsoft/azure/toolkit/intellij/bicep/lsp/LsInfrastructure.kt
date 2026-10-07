@file:Suppress("UnstableApiUsage")

package com.microsoft.azure.toolkit.intellij.bicep.lsp

import com.intellij.openapi.util.NlsSafe
import com.intellij.util.concurrency.annotations.RequiresBackgroundThread
import com.intellij.util.concurrency.annotations.RequiresReadLockAbsence
import org.jetbrains.annotations.Nls
import java.nio.file.Path
import kotlin.io.path.exists

internal interface LsInfrastructure {
    val formattedUrl: @NlsSafe String?
    val extractedDirectoryName: @NlsSafe String
    val executableName: @NlsSafe String
    val presentableName: @Nls String

    @RequiresBackgroundThread
    @RequiresReadLockAbsence
    fun isPresent(): Boolean {
        val executablePath = findExecutablePath()
        return executablePath.exists()
    }

    @RequiresBackgroundThread
    @RequiresReadLockAbsence
    fun isValid(): Boolean {
        return isPresent()
    }

    @RequiresBackgroundThread
    @RequiresReadLockAbsence
    fun findExecutablePath(): Path

    @RequiresBackgroundThread
    @RequiresReadLockAbsence
    fun patchInfrastructureFiles() {
    }

    companion object {
        fun allKnown() = sequenceOf(BicepLS)
    }
}

