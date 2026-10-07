/*
 * Copyright 2018-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the MIT license.
 */

@file:Suppress("DuplicatedCode")

package com.microsoft.azure.toolkit.intellij.legacy.appservice

import com.intellij.openapi.Disposable
import com.intellij.openapi.util.Disposer
import com.intellij.ui.components.JBRadioButton
import com.intellij.ui.dsl.builder.ButtonsGroup
import com.intellij.ui.dsl.builder.Cell
import com.intellij.ui.dsl.builder.bind
import com.intellij.ui.dsl.builder.panel
import com.microsoft.azure.toolkit.intellij.common.AzureFormPanel
import com.microsoft.azure.toolkit.lib.Azure
import com.microsoft.azure.toolkit.lib.appservice.config.AppServiceConfig
import com.microsoft.azure.toolkit.lib.appservice.config.RuntimeConfig
import com.microsoft.azure.toolkit.lib.appservice.model.OperatingSystem
import com.microsoft.azure.toolkit.lib.appservice.model.Runtime
import com.microsoft.azure.toolkit.lib.auth.AzureAccount
import com.microsoft.azure.toolkit.lib.common.form.AzureFormInput
import java.util.function.Supplier
import javax.swing.JPanel

class AppServiceInfoBasicPanel<T>(
    targetProjectOnNetFramework: Boolean = false,
    private val defaultConfigSupplier: Supplier<T>
) : JPanel(), Disposable, AzureFormPanel<T> where T : AppServiceConfig {

    private var config: T?

    private val panel: JPanel
    private val textName = AppNameInput().apply {
        isRequired = true
    }

    private var operatingSystem = OperatingSystem.WINDOWS
    private lateinit var operatingSystemGroup: ButtonsGroup
    private lateinit var windowsRadioButton: Cell<JBRadioButton>
    private lateinit var linuxRadioButton: Cell<JBRadioButton>
    private lateinit var dockerRadioButton: Cell<JBRadioButton>

    init {
        Disposer.register(this, textName)

        panel = panel {
            group("Instance Details") {
                row("Name:") {
                    cell(textName)
                    label(".azurewebsites.net")
                }
                operatingSystemGroup = buttonsGroup {
                    row("Operating System:") {
                        windowsRadioButton = radioButton("Windows", OperatingSystem.WINDOWS)
                        linuxRadioButton = radioButton("Linux", OperatingSystem.LINUX)
                            .enabled(!targetProjectOnNetFramework)
                        dockerRadioButton = radioButton("Docker", OperatingSystem.DOCKER)
                            .visible(false)
                    }
                }.bind(::operatingSystem)
            }
        }

        add(panel)

        config = defaultConfigSupplier.get().also { setValue(it) }
    }

    override fun getValue(): T {
        val name = textName.value
        val operatingSystem =
            if (windowsRadioButton.component.isSelected) OperatingSystem.WINDOWS
            else if (linuxRadioButton.component.isSelected) OperatingSystem.LINUX
            else OperatingSystem.DOCKER

        val account = Azure.az(AzureAccount::class.java).account()
        val subscription = config
            ?.let { account.getSubscription(it.subscriptionId) }
            ?: account.selectedSubscriptions.firstOrNull()

        val result = config ?: defaultConfigSupplier.get()
        result.appName = name
        subscription?.let { result.subscriptionId = it.id }
        result.runtime = (result.runtime ?: RuntimeConfig()).apply {
            os = operatingSystem
        }

        config = result

        return result
    }

    override fun setValue(config: T) {
        this.config = config

        val subscription =
            config.subscriptionId?.let { Azure.az(AzureAccount::class.java).account().getSubscription(it) }

        textName.value = config.appName
        textName.setSubscription(subscription)
        when (config.runtime.os) {
            OperatingSystem.WINDOWS -> windowsRadioButton.component.isSelected = true
            OperatingSystem.LINUX -> linuxRadioButton.component.isSelected = true
            OperatingSystem.DOCKER -> dockerRadioButton.component.isSelected = true
            else -> linuxRadioButton.component.isSelected = true
        }
    }

    override fun getInputs(): List<AzureFormInput<*>> = listOf(textName)

    override fun setVisible(visible: Boolean) {
        panel.isVisible = visible
        @Suppress("RemoveExplicitSuperQualifier")
        super<JPanel>.setVisible(visible)
    }

    fun setFixedRuntime(runtime: Runtime) {
        when (runtime.operatingSystem) {
            OperatingSystem.WINDOWS -> {
                windowsRadioButton.component.isSelected = true
            }

            OperatingSystem.LINUX -> {
                linuxRadioButton.component.isSelected = true
            }

            OperatingSystem.DOCKER -> {
                dockerRadioButton.component.isSelected = true
            }

            else -> {
                linuxRadioButton.component.isSelected = true
            }
        }
        operatingSystemGroup.visible(false)
    }

    override fun dispose() {
    }
}