package com.microsoft.intellij.util

import com.microsoft.intellij.util.PluginUtil.getPluginRootDirectory
import java.io.File

object PluginHelper {
    fun getTemplateFile(fileName: String) = String.format("%s%s%s", getPluginRootDirectory(), File.separator, fileName)
}