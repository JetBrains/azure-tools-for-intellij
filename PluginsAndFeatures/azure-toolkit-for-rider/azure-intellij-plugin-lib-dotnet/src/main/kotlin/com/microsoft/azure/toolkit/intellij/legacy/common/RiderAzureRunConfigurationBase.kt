/*
 * Copyright 2018-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the MIT license.
 */

package com.microsoft.azure.toolkit.intellij.legacy.common

import com.intellij.execution.configurations.ConfigurationFactory
import com.intellij.execution.configurations.LocatableConfigurationBase
import com.intellij.openapi.options.ConfigurationException
import com.intellij.openapi.project.Project
import com.intellij.util.xmlb.XmlSerializer
import com.microsoft.azure.toolkit.intellij.common.auth.AzureLoginHelper
import com.microsoft.azure.toolkit.lib.common.exception.AzureExecutionException
import org.jdom.Element

abstract class RiderAzureRunConfigurationBase<T : Any>(project: Project, factory: ConfigurationFactory, name: String?) :
        LocatableConfigurationBase<T>(project, factory, name) {
    abstract fun getModel(): T

    abstract fun validate()

    override fun readExternal(element: Element) {
        super.readExternal(element)
        XmlSerializer.deserializeInto(getModel(), element)
    }

    override fun writeExternal(element: Element) {
        super.writeExternal(element)
        XmlSerializer.serializeInto(getModel(), element)
    }

    protected fun checkAzurePreconditions() {
        try {
            AzureLoginHelper.ensureAzureSubsAvailable()
        } catch (e: AzureExecutionException) {
            throw ConfigurationException(e.message)
        }
    }
}