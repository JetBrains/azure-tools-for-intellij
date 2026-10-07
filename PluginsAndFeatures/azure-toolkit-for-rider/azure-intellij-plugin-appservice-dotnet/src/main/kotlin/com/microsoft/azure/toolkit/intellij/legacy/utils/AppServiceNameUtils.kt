/*
 * Copyright 2018-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the MIT license.
 */

package com.microsoft.azure.toolkit.intellij.legacy.utils

//See: https://learn.microsoft.com/en-us/azure/azure-resource-manager/management/resource-name-rules#microsoftweb
private val applicationNameRegex = Regex("^[a-zA-Z\\d-]*$")
internal const val APPLICATION_VALIDATION_MESSAGE = "App names only allow alphanumeric, hyphen characters, cannot start or end in a hyphen, and must be less than 60 chars"
private val invalidCharRegex = Regex("[^a-zA-Z\\d-]")

internal fun isValidApplicationName(name: String?): Boolean {
    if (name.isNullOrEmpty() || name.length < 2 || name.length > 60) return false
    if (name.startsWith('-') || name.endsWith('-')) return false
    return applicationNameRegex.matches(name)
}

internal fun removeInvalidCharacters(name: String): String {
    return name.replace(invalidCharRegex, "")
}

//See: https://learn.microsoft.com/en-us/azure/azure-resource-manager/management/resource-name-rules#microsoftweb
private val applicationSlotNameRegex = Regex("^[a-zA-Z\\d-]*$")
internal const val APPLICATION_SLOT_VALIDATION_MESSAGE = "App slot names only allow alphanumeric, hyphen characters, cannot start or end in a hyphen, and must be less than 59 chars"

internal fun isValidApplicationSlotName(name: String?): Boolean {
    if (name.isNullOrEmpty() || name.length < 2 || name.length > 59) return false
    if (name.startsWith('-') || name.endsWith('-')) return false
    return applicationSlotNameRegex.matches(name)
}

//See: https://learn.microsoft.com/en-us/azure/azure-resource-manager/management/resource-name-rules#microsoftresources
private val resourceGroupNameRegex = Regex("^[a-zA-Z\\d_.\\-()]*$")
internal const val RESOURCE_GROUP_VALIDATION_MESSAGE = "Resource group names only allow alphanumeric, underscore, hyphen, period (except at end), parentheses characters, and must be less than 90 chars"

internal fun isValidResourceGroupName(name: String?): Boolean {
    if (name.isNullOrEmpty() || name.length > 90) return false
    if (name.endsWith('.')) return false
    return resourceGroupNameRegex.matches(name)
}
