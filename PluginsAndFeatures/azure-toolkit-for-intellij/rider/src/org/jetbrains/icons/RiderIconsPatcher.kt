/**
 * Copyright (c) 2020-2023 JetBrains s.r.o.
 *
 * All rights reserved.
 *
 * MIT License
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation files (the "Software"), to deal in the Software without restriction, including without limitation
 * the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and
 * to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of
 * the Software.
 *
 * THE SOFTWARE IS PROVIDED *AS IS*, WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO
 * THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT,
 * TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package org.jetbrains.icons

import com.intellij.httpClient.RestClientIcons
import com.intellij.openapi.util.CachedImageIcon
import com.intellij.openapi.util.IconLoader
import com.intellij.openapi.util.IconPathPatcher
import icons.CommonIcons
import javax.swing.Icon

/**
 * Icons Patcher for icons set from Rider backend (R#).
 * Rider backend do not have access to fronted icons (e.g. FunctionApp.svg, new.svg, etc.). To share an existing frontend icons
 * and reuse them instead of creating a duplicate set for a backend, we can replace a fake backend icon with any frontend icon by path.
 */
internal class RiderIconsPatcher : IconPathPatcher() {

    companion object {
        fun install() = myInstallPatcher

        private val myInstallPatcher: Unit by lazy {
            IconLoader.installPathPatcher(RiderIconsPatcher())
        }

        private data class IconAndClassLoader(val icon: Icon?, val classLoader: ClassLoader?)

        private fun common(icon: Icon): IconAndClassLoader {
            return IconAndClassLoader(icon, CommonIcons::class.java.classLoader)
        }

        private fun restClient(icon: Icon): IconAndClassLoader {
            return IconAndClassLoader(icon, RestClientIcons::class.java.classLoader)
        }

        private fun normalize(path: String) : String {
            if (!path.startsWith("/"))
                return "/$path"

            return path
        }
    }

    override fun patchPath(path: String, classLoader: ClassLoader?): String? {
        val iconAndClassLoader = myIconsOverrideMap[normalize(path)] ?: return null
        return (iconAndClassLoader.icon as? CachedImageIcon)?.originalPath
    }

    override fun getContextClassLoader(path: String, originalClassLoader: ClassLoader?): ClassLoader? {
        val iconAndClassLoader = myIconsOverrideMap[normalize(path)] ?: return null
        return iconAndClassLoader.classLoader ?: originalClassLoader
    }

    private val myIconsOverrideMap = mapOf(
            "/resharper/FunctionAppRunMarkers/RunFunctionApp.svg" to common(CommonIcons.AzureFunctions.FunctionAppRunConfiguration),
            "/resharper/FunctionAppRunMarkers/Trigger.svg" to restClient(RestClientIcons.Http_requests_filetype),
            "/resharper/FunctionAppTemplates/AzureFunctionsTrigger.svg" to common(CommonIcons.AzureFunctions.FunctionApp)
    )
}
