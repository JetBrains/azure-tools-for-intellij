/*
 * Copyright 2018-2025 JetBrains s.r.o. and contributors. Use of this source code is governed by the MIT license.
 */

@file:Suppress("DialogTitleCapitalization", "UnstableApiUsage", "DuplicatedCode")

package com.microsoft.azure.toolkit.intellij.legacy.function.runner.functionAppContainer

import com.intellij.execution.Executor
import com.intellij.execution.configurations.ConfigurationFactory
import com.intellij.execution.configurations.LocatableConfigurationBase
import com.intellij.execution.configurations.RuntimeConfigurationError
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.openapi.project.Project
import com.intellij.platform.util.coroutines.childScope
import com.microsoft.azure.toolkit.intellij.AppServiceProjectService
import com.microsoft.azure.toolkit.intellij.legacy.utils.isAccountSignedIn

class FunctionAppContainerConfiguration(private val project: Project, factory: ConfigurationFactory, name: String?) :
    LocatableConfigurationBase<FunctionAppContainerConfigurationOptions>(project, factory, name) {

    companion object {
        private const val REPO_COMPONENT_REGEX_PATTERN = "[a-z0-9]+(?:[._-][a-z0-9]+)*"
        private const val TAG_REGEX_PATTERN = "^\\w+[\\w.-]*$"
    }

    private val repoComponentRegex = Regex(REPO_COMPONENT_REGEX_PATTERN)
    private val tagRegex = Regex(TAG_REGEX_PATTERN)

    override fun suggestedName() = "Publish Function App Container"

    override fun getState() = options as? FunctionAppContainerConfigurationOptions

    override fun getState(executor: Executor, environment: ExecutionEnvironment) =
        FunctionAppContainerDeploymentState(
            project,
            AppServiceProjectService.getInstance(project).scope.childScope("FunctionAppContainerDeploymentState"),
            this
        )

    override fun getConfigurationEditor() = FunctionAppContainerSettingEditor(project)

    override fun checkConfiguration() {
        val options = getState() ?: return
        with(options) {
            isAccountSignedIn()

            if (functionAppName.isNullOrEmpty()) throw RuntimeConfigurationError("Function App name is not provided")
            if (subscriptionId.isNullOrEmpty()) throw RuntimeConfigurationError("Subscription is not provided")
            if (resourceGroupName.isNullOrEmpty()) throw RuntimeConfigurationError("Resource group is not provided")
            if (region.isNullOrEmpty()) throw RuntimeConfigurationError("Region is not provided")
            if (appServicePlanName.isNullOrEmpty()) throw RuntimeConfigurationError("App Service plan name is not provided")
            if (appServicePlanResourceGroupName.isNullOrEmpty()) throw RuntimeConfigurationError("App Service plan resource group is not provided")
            if (pricingTier.isNullOrEmpty()) throw RuntimeConfigurationError("Pricing tier is not provided")
            if (pricingSize.isNullOrEmpty()) throw RuntimeConfigurationError("Pricing size is not provided")

            val repository = imageRepository
            if (repository.isNullOrEmpty()) throw RuntimeConfigurationError("Image repository is not provided")
            if (repository.length > 255) throw RuntimeConfigurationError("The length of image repository must be less than 256 characters")
            if (repository.endsWith('/')) throw RuntimeConfigurationError("The repository name should not end with '/'")
            val tag = imageTag
            if (tag.isNullOrEmpty()) throw RuntimeConfigurationError("Image tag is not provided")
            if (tag.length > 127) throw RuntimeConfigurationError("The length of tag name must be less than 128 characters")
            if (!tagRegex.matches(tag)) throw RuntimeConfigurationError("Invalid tag: $tag, should follow: $TAG_REGEX_PATTERN")
            val repositoryParts = repository.split('/')
            if (repositoryParts.last().isEmpty()) throw RuntimeConfigurationError("Image name is not provided")
            repositoryParts.forEach {
                if (!repoComponentRegex.matches(it)) throw RuntimeConfigurationError("Invalid repository component: $it, should follow: $REPO_COMPONENT_REGEX_PATTERN")
            }
        }
    }
}
