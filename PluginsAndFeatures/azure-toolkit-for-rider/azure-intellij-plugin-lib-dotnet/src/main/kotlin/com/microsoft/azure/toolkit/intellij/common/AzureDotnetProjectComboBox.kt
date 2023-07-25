@file:Suppress("UnstableApiUsage")

package com.microsoft.azure.toolkit.intellij.common

import com.intellij.icons.AllIcons
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.IconLoader
import com.intellij.ui.LayeredIcon
import com.intellij.ui.SimpleListCellRenderer
import com.intellij.workspaceModel.ide.WorkspaceModel
import com.jetbrains.rider.model.PublishableProjectModel
import com.jetbrains.rider.model.publishableProjectsModel
import com.jetbrains.rider.projectView.calculateIcon
import com.jetbrains.rider.projectView.solution
import com.jetbrains.rider.projectView.workspace.getProjectModelEntities
import com.jetbrains.rider.projectView.workspace.isProject
import com.jetbrains.rider.projectView.workspace.isUnloadedProject
import org.apache.commons.lang3.StringUtils
import java.nio.file.Path
import javax.swing.Icon
import javax.swing.JList

class AzureDotnetProjectComboBox(
        private val project: Project,
        private val canBePublishedAction: (PublishableProjectModel) -> Boolean
) : AzureComboBox<PublishableProjectModel>() {
    init {
        renderer = DotnetProjectItemRenderer()
    }

    override fun loadItems(): List<PublishableProjectModel> {
        return project.solution.publishableProjectsModel.publishableProjects.values.toList()
    }

    override fun getItemText(item: Any?): String =
            if (item is PublishableProjectModel) {
                item.projectName
            } else {
                StringUtils.EMPTY
            }

    override fun getItemIcon(item: Any?): Icon? =
            if (item is PublishableProjectModel) {
                calculateIcon(item)
            } else {
                null
            }

    private fun calculateIcon(projectModel: PublishableProjectModel): Icon? {
        val projectNodes = WorkspaceModel.getInstance(project)
                .getProjectModelEntities(Path.of(projectModel.projectFilePath), project)
                .filter { it.isProject() || it.isUnloadedProject() }
        if (projectNodes.isEmpty()) return null

        val itemIcon = projectNodes[0].calculateIcon(project) ?: return null

        return if (canBePublishedAction(projectModel))
            itemIcon
        else
            LayeredIcon.create(IconLoader.getDisabledIcon(itemIcon), AllIcons.RunConfigurations.InvalidConfigurationLayer)
    }

    inner class DotnetProjectItemRenderer : SimpleListCellRenderer<PublishableProjectModel>() {
        override fun customize(
                list: JList<out PublishableProjectModel>,
                projectModel: PublishableProjectModel?,
                index: Int,
                selected: Boolean,
                hasFocus: Boolean
        ) {
            if (project.isDisposed) return

            if (projectModel == null) {
                text = "No projects to publish"
                return
            }

            text = projectModel.projectName
            val projectIcon = calculateIcon(projectModel)
            if (projectIcon != null) icon = projectIcon
        }
    }
}