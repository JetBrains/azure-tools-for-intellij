/*
 * Copyright 2018-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the MIT license.
 */

@file:Suppress("UnstableApiUsage", "PropertyName")

package com.microsoft.azure.toolkit.intellij.appservice.deployment

import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.project.Project
import com.intellij.platform.util.coroutines.childScope
import com.jetbrains.rider.model.PublishableProjectModel
import com.jetbrains.rider.model.publishableProjectsModel
import com.jetbrains.rider.projectView.SolutionConfigurationManager
import com.jetbrains.rider.projectView.solution
import com.jetbrains.rider.run.configurations.publishing.PublishRuntimeSettingsCoreHelper.ConfigurationAndPlatform
import com.microsoft.azure.toolkit.intellij.appservice.deployment.AppServiceDeploymentModel.DraftAppServiceModel
import com.microsoft.azure.toolkit.intellij.appservice.deployment.AppServiceDeploymentModel.RemoteAppServiceModel
import com.microsoft.azure.toolkit.intellij.appservice.utils.isSameApp
import com.microsoft.azure.toolkit.intellij.legacy.availableTargetFrameworks
import com.microsoft.azure.toolkit.intellij.legacy.targetsNetFramework
import com.microsoft.azure.toolkit.lib.Azure
import com.microsoft.azure.toolkit.lib.appservice.AzureAppService
import com.microsoft.azure.toolkit.lib.appservice.config.AppServiceConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.*
import kotlin.coroutines.cancellation.CancellationException

abstract class AbstractAppServiceDeploymentViewModel<TConfig : AppServiceConfig>(
    private val project: Project,
    parentCs: CoroutineScope,
    publishableProjectFilter: (PublishableProjectModel) -> Boolean
) : AppServiceDeploymentViewModel<TConfig> {
    companion object {
        private val LOG = logger<AbstractAppServiceDeploymentViewModel<*>>()
    }

    protected val cs = parentCs.childScope("AbstractAppServiceDeploymentViewModel", Dispatchers.Default)

    protected val _draftAppServiceState = MutableStateFlow<List<DraftAppServiceModel<TConfig>>>(emptyList())
    override val draftAppServiceState: StateFlow<List<DraftAppServiceModel<TConfig>>> = _draftAppServiceState.asStateFlow()

    protected val _remoteAppServiceState = MutableStateFlow<AppServiceLoadState<TConfig>>(AppServiceLoadState.Loading)
    override val remoteAppServiceState: StateFlow<AppServiceLoadState<TConfig>> = _remoteAppServiceState.asStateFlow()

    protected val _selectedAppService = MutableStateFlow<Pair<TConfig, String?>?>(null)
    override val selectedAppService: StateFlow<Pair<TConfig, String?>?> = _selectedAppService.asStateFlow()

    protected val _openBrowserAfterDeployment = MutableStateFlow(false)
    val openBrowserAfterDeployment: StateFlow<Boolean> = _openBrowserAfterDeployment.asStateFlow()

    protected val _publishableProjects = MutableStateFlow<List<PublishableProjectModel>>(emptyList())
    val publishableProjects: StateFlow<List<PublishableProjectModel>> = _publishableProjects.asStateFlow()

    private val _selectedProject = MutableStateFlow<PublishableProjectModel?>(null)
    val selectedProject: StateFlow<PublishableProjectModel?> = _selectedProject.asStateFlow()

    private val _targetFrameworks = MutableStateFlow<List<String>>(emptyList())
    val targetFrameworks: StateFlow<List<String>> = _targetFrameworks.asStateFlow()

    private val _selectedTargetFramework = MutableStateFlow<String?>(null)
    val selectedTargetFramework: StateFlow<String?> = _selectedTargetFramework.asStateFlow()

    // Target framework is passed to MSBuild only for .NET (Core) projects, same as in Rider's Publish to Folder
    private val _isTargetFrameworkVisible = MutableStateFlow(false)
    val isTargetFrameworkVisible: StateFlow<Boolean> = _isTargetFrameworkVisible.asStateFlow()

    private val _isNetFramework = MutableStateFlow(false)
    open override val isNetFramework: StateFlow<Boolean> = _isNetFramework.asStateFlow()

    protected val _configurationAndPlatforms = MutableStateFlow<List<ConfigurationAndPlatform>>(emptyList())
    val configurationAndPlatforms: StateFlow<List<ConfigurationAndPlatform>> = _configurationAndPlatforms.asStateFlow()

    val selectedConfigurationAndPlatform = MutableStateFlow<ConfigurationAndPlatform?>(null)

    protected val reloadTrigger = MutableSharedFlow<Boolean>(replay = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)

    init {
        _publishableProjects.value = project.solution.publishableProjectsModel.publishableProjects.values
            .filter { publishableProjectFilter(it) }
        selectProject(_publishableProjects.value.firstOrNull())

        val manager = SolutionConfigurationManager.tryGetInstance(project)
        _configurationAndPlatforms.value = manager?.solutionConfigurationsAndPlatforms.orEmpty()
            .sortedBy { it.configuration.lowercase(Locale.ROOT) + "_" + it.platform.lowercase(Locale.ROOT) }
            .map { ConfigurationAndPlatform(it.configuration, it.platform) }
        selectedConfigurationAndPlatform.value =
            _configurationAndPlatforms.value.firstOrNull { it.configuration.contains("Release") }
                ?: configurationAndPlatforms.value.firstOrNull()

        reloadTrigger.tryEmit(false)

        cs.launch {
            combine(remoteAppServiceState, selectedAppService) { state, selected ->
                state to selected
            }.collect { (state, selected) ->
                if (state is AppServiceLoadState.Loaded) {
                    val remote = state.items
                    _draftAppServiceState.update { drafts ->
                        //Remove from drafts all the existing remote apps
                        val filteredDrafts =
                            drafts.filter { draft -> remote.none { isSameApp(it.config, draft.config) } }

                        if (selected == null ||
                            remote.any { isSameApp(it.config, selected.first) } ||
                            drafts.any { isSameApp(it.config, selected.first) }
                        ) {
                            filteredDrafts
                        } else {
                            //If we cannot find selected app among remote apps and draft apps, add it to the drafts
                            listOf(DraftAppServiceModel(selected.first)) + filteredDrafts
                        }
                    }
                }
            }
        }

        cs.launch {
            reloadTrigger
                .collectLatest { refresh ->
                    _remoteAppServiceState.value = AppServiceLoadState.Loading

                    if (refresh) invalidateAppCache()
                    try {
                        val configs = loadListOfApps()
                        _remoteAppServiceState.value = AppServiceLoadState.Loaded(configs)
                    } catch (e: CancellationException) {
                        throw e
                    } catch (e: Exception) {
                        LOG.warn("Error while trying to load Azure web apps", e)
                        _remoteAppServiceState.value = AppServiceLoadState.Error(e.message ?: "Unknown error")
                    }
                }
        }
    }

    override fun selectAppService(appService: AppServiceDeploymentModel<TConfig>?, deploymentSlotName: String?) {
        _selectedAppService.value = appService?.let { it.config to deploymentSlotName }
    }

    override fun refreshAppServices() {
        reloadTrigger.tryEmit(true)
    }

    override fun addDraftAppService(config: TConfig) {
        val model = DraftAppServiceModel(config)
        _draftAppServiceState.update { current ->
            listOf(model) + current.filter { !isSameApp(it.config, model.config) }
        }
        _selectedAppService.value = config to null
    }

    fun selectProject(publishableProject: PublishableProjectModel?) {
        val frameworks = publishableProject?.availableTargetFrameworks().orEmpty()
        // Keep the current selection when switching projects if the new project also targets this framework
        _selectedTargetFramework.value =
            _selectedTargetFramework.value?.takeIf { it in frameworks } ?: frameworks.firstOrNull()
        _targetFrameworks.value = frameworks
        _isTargetFrameworkVisible.value = publishableProject?.isDotNetCore == true
        _selectedProject.value = publishableProject
        updateIsNetFramework()
    }

    fun selectTargetFramework(targetFramework: String?) {
        _selectedTargetFramework.value = targetFramework
        updateIsNetFramework()
    }

    private fun updateIsNetFramework() {
        _isNetFramework.value = _selectedProject.value
            ?.targetsNetFramework(project, _selectedTargetFramework.value)
            ?: false
    }

    fun setOpenBrowserFlag(enabled: Boolean) {
        _openBrowserAfterDeployment.value = enabled
    }

    protected abstract suspend fun loadListOfApps(): List<RemoteAppServiceModel<TConfig>>
    protected abstract fun invalidateAppCache()

    protected suspend fun loadAppServicePlans() {
        val appServicePlans = Azure.az(AzureAppService::class.java).plans()
        coroutineScope {
            appServicePlans.forEach {
                launch {
                    it.remote
                }
            }
        }
    }
}
