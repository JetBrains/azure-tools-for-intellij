/*
 * Copyright 2018-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the MIT license.
 */

package com.microsoft.azure.toolkit.intellij.common

import com.intellij.docker.DockerIcons
import com.intellij.docker.agent.DockerAuthConfig
import com.intellij.docker.registry.DockerRegistryConfiguration
import com.intellij.docker.registry.DockerRegistryListConfigurable
import com.intellij.docker.registry.DockerRegistryManager
import com.intellij.icons.AllIcons
import com.intellij.openapi.keymap.KeymapUtil
import com.intellij.openapi.options.ShowSettingsUtil
import com.intellij.openapi.project.Project
import com.intellij.ui.SimpleListCellRenderer
import com.intellij.ui.components.fields.ExtendableTextComponent.*
import java.awt.event.InputEvent
import java.awt.event.KeyEvent
import javax.swing.JList
import javax.swing.KeyStroke

class AzureContainerRegistryComboBox(private val project: Project) : AzureComboBox<ContainerRegistryModel>() {
    init {
        renderer = ContainerRegistryRenderer()

        project.messageBus.connect()
            .subscribe(DockerRegistryManager.Listener.TOPIC, object : DockerRegistryManager.Listener {
                override fun registryAdded(registry: DockerRegistryConfiguration) {
                    reloadItems()
                }

                override fun registryRemoved(registry: DockerRegistryConfiguration) {
                    reloadItems()
                }
            })
    }

    fun setRegistry(registryAddress: String) {
        reloadItems()
        setValue { it.address == registryAddress }
    }

    override fun loadItems(): List<ContainerRegistryModel> {
        val registries = DockerRegistryManager.getInstance().registries
        return registries.map {
            ContainerRegistryModel(it.name, it.address, it.username, it.authConfig)
        }
    }

    override fun getItemText(item: Any?) =
        if (item is ContainerRegistryModel) {
            item.name
        } else {
            ""
        }

    override fun getItemIcon(item: Any?) =
        if (item is ContainerRegistryModel) {
            DockerIcons.DockerRegistry
        } else {
            null
        }

    override fun getExtensions(): List<Extension?> {
        val extensions = super.getExtensions() as MutableList<Extension?>

        val keyStroke = KeyStroke.getKeyStroke(KeyEvent.VK_INSERT, InputEvent.ALT_DOWN_MASK)
        val tooltip = "Create new container registry ${KeymapUtil.getKeystrokeText(keyStroke)}"
        val addEx = Extension.create(AllIcons.General.Add, tooltip, ::showContainerRegistryCreationPopup)
        registerShortcut(keyStroke, addEx)
        extensions.add(addEx)

        return extensions
    }

    private fun showContainerRegistryCreationPopup() {
        val configurable = DockerRegistryListConfigurable()
        ShowSettingsUtil.getInstance().editConfigurable(project, configurable)
    }

    inner class ContainerRegistryRenderer : SimpleListCellRenderer<ContainerRegistryModel>() {
        override fun customize(
            list: JList<out ContainerRegistryModel>,
            registry: ContainerRegistryModel?,
            index: Int,
            selected: Boolean,
            hasFocus: Boolean
        ) {
            if (registry == null) {
                text = "No registries"
                return
            }

            text = registry.name
            icon = DockerIcons.DockerRegistry
        }
    }
}

data class ContainerRegistryModel(
    val name: String,
    val address: String,
    val username: String,
    val authConfig: DockerAuthConfig
)