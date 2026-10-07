/*
 * Copyright 2018-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the MIT license.
 */

@file:Suppress("UnstableApiUsage")

package com.microsoft.azure.toolkit.intellij.icons

import com.intellij.httpClient.RestClientIcons
import com.intellij.openapi.util.IconLoader
import com.intellij.openapi.util.IconPathPatcher
import com.intellij.ui.ExperimentalUI
import com.intellij.ui.icons.CachedImageIcon
import com.microsoft.azure.toolkit.ide.common.icon.AzureIcons
import com.microsoft.azure.toolkit.intellij.common.IntelliJAzureIcons
import javax.swing.Icon

/**
 * Icon Patcher for icons set from Rider backend (R#).
 * Rider backend does not have access to frontend icons (e.g. FunctionApp.svg, new.svg, etc.). To share existing frontend icons
 * and reuse them instead of creating a duplicate set for a backend, we can replace a fake backend icon with any frontend icon by path.
 */
class RiderIconPatcher : IconPathPatcher() {
    companion object {
        fun install() = myInstallPatcher

        private val myInstallPatcher: Unit by lazy {
            IconLoader.installPathPatcher(RiderIconPatcher())
        }

        private data class IconAndClassLoader(val icon: Icon?, val classLoader: ClassLoader?)

        private fun azure(icon: Icon): IconAndClassLoader {
            return IconAndClassLoader(icon, IntelliJAzureIcons::class.java.classLoader)
        }

        private fun restClient(icon: Icon): IconAndClassLoader {
            return IconAndClassLoader(icon, RestClientIcons::class.java.classLoader)
        }

        private fun normalize(path: String) = if (path.startsWith("/")) path else "/$path"
    }

    override fun patchPath(path: String, classLoader: ClassLoader?): String? {
        val iconAndClassLoader = myIconsOverrideMap[normalize(path)] ?: return null
        return (iconAndClassLoader.icon as? CachedImageIcon)?.let {
            if (ExperimentalUI.isNewUI() && it.expUIPath != null) {
                it.expUIPath
            } else {
                it.originalPath
            }
        }
    }

    override fun getContextClassLoader(path: String, originalClassLoader: ClassLoader?): ClassLoader? {
        val iconAndClassLoader = myIconsOverrideMap[normalize(path)] ?: return null
        return iconAndClassLoader.classLoader ?: originalClassLoader
    }

    private val myIconsOverrideMap = mapOf(
        "/resharper/FunctionAppRunMarkers/RunFunctionApp.svg" to azure(IntelliJAzureIcons.getIcon("/icons/FunctionApp/Run.svg")),
        "/resharper/FunctionAppRunMarkers/Trigger.svg" to restClient(RestClientIcons.Http_requests_filetype),
        "/resharper/FunctionAppTemplates/AzureFunctionsTrigger.svg" to azure(IntelliJAzureIcons.getIcon(AzureIcons.FunctionApp.MODULE))
    )
}
