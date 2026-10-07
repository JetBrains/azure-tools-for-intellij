/*
 * Copyright 2018-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the MIT license.
 */

package com.microsoft.azure.toolkit.intellij.common

import javax.swing.JComboBox

fun <T> JComboBox<T>.getSelectedValue(): T? {
    val index = this.selectedIndex
    if (index == -1) return null
    return getItemAt(index)
}