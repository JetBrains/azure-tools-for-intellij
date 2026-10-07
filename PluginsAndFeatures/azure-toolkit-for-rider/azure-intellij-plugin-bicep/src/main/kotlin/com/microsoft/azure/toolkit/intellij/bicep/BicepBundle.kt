/*
 * Copyright 2018-2025 JetBrains s.r.o. and contributors. Use of this source code is governed by the MIT license.
 */

package com.microsoft.azure.toolkit.intellij.bicep

import com.intellij.DynamicBundle
import com.intellij.openapi.util.NlsSafe
import org.jetbrains.annotations.Nls
import org.jetbrains.annotations.PropertyKey
import java.util.function.Supplier

private const val PATH_TO_BUNDLE = "messages.BicepBundle"

object BicepBundle : DynamicBundle(PATH_TO_BUNDLE) {
  const val BICEP_EXTENSION: @NlsSafe String = "bicep"

  @Nls
  @JvmStatic
  fun message(@PropertyKey(resourceBundle = PATH_TO_BUNDLE) key: String, vararg params: Any): String = getMessage(key, *params)

  @Suppress("unused")
  @JvmStatic
  fun messagePointer(@PropertyKey(resourceBundle = PATH_TO_BUNDLE) key: String, vararg params: Any): Supplier<@Nls String> {
    return getLazyMessage(key, *params)
  }
}