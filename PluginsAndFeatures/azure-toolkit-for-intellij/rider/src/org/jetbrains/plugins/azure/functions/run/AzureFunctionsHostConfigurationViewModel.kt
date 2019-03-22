/**
 * Copyright (c) 2018 JetBrains s.r.o.
 * <p/>
 * All rights reserved.
 * <p/>
 * MIT License
 * <p/>
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation files (the "Software"), to deal in the Software without restriction, including without limitation
 * the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and
 * to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 * <p/>
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of
 * the Software.
 * <p/>
 * THE SOFTWARE IS PROVIDED *AS IS*, WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO
 * THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT,
 * TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package org.jetbrains.plugins.azure.functions.run

import com.intellij.util.execution.ParametersListUtil
import com.jetbrains.rd.util.lifetime.Lifetime
import com.jetbrains.rd.util.reactive.adviseOnce
import com.jetbrains.rider.model.*
import com.jetbrains.rider.run.configurations.controls.*
import com.jetbrains.rider.run.configurations.controls.startBrowser.BrowserSettings
import com.jetbrains.rider.run.configurations.controls.startBrowser.BrowserSettingsEditor
import com.jetbrains.rider.run.configurations.dotNetExe.DotNetExeConfigurationViewModel
import com.jetbrains.rider.run.configurations.project.DotNetStartBrowserParameters
import java.io.File

class AzureFunctionsHostConfigurationViewModel(
        private val lifetime: Lifetime,
        private val runnableProjectsModel: RunnableProjectsModel,
        val projectSelector: ProjectSelector,
        val tfmSelector: StringSelector,
        programParametersEditor: ProgramParametersEditor,
        workingDirectorySelector: PathSelector,
        environmentVariablesEditor: EnvironmentVariablesEditor,
        useExternalConsoleEditor: FlagEditor,
        val separator: ViewSeparator,
        val urlEditor: TextEditor,
        val dotNetBrowserSettingsEditor: BrowserSettingsEditor
) : DotNetExeConfigurationViewModel(
        lifetime,
        PathSelector("", null, lifetime),
        programParametersEditor,
        workingDirectorySelector,
        environmentVariablesEditor,
        FlagEditor(""),
        ProgramParametersEditor("", lifetime),
        false,
        useExternalConsoleEditor) {

    override val controls: List<ControlBase> = listOf(
            projectSelector,
            tfmSelector,
            programParametersEditor,
            workingDirectorySelector,
            environmentVariablesEditor,
            useExternalConsoleEditor,
            separator,
            urlEditor,
            dotNetBrowserSettingsEditor
    )

    private var isLoaded = false
    private val type = AzureFunctionsHostConfigurationType()

    var trackProjectExePath = true
    var trackProjectArguments = true
    var trackProjectWorkingDirectory = true
    private val portRegex = Regex("--port (\\d+)", RegexOption.IGNORE_CASE)

    init {
        disable()
        projectSelector.bindTo(runnableProjectsModel, lifetime, { p -> type.isApplicable(p.kind) }, ::enable, ::handleProjectSelection)

        tfmSelector.string.advise(lifetime) { handleChangeTfmSelection() }
        exePathSelector.path.advise(lifetime) { recalculateTrackProjectOutput() }
        programParametersEditor.parametersString.advise(lifetime) { recalculateTrackProjectOutput() }
        workingDirectorySelector.path.advise(lifetime) { recalculateTrackProjectOutput() }
    }

    private fun handleChangeTfmSelection() {
        projectSelector.project.valueOrNull?.projectOutputs?.singleOrNull { it.tfm == tfmSelector.string.valueOrNull }?.let {
            val shouldChangeExePath = trackProjectExePath
            val shouldChangeWorkingDirectory = trackProjectWorkingDirectory
            if (shouldChangeExePath) {
                exePathSelector.path.set(it.exePath)
            }
            if (shouldChangeWorkingDirectory) {
                workingDirectorySelector.path.set(it.workingDirectory)
            }
            exePathSelector.defaultValue.set(it.exePath)
            if (it.defaultArguments.isNotEmpty()) {
                programParametersEditor.parametersString.set(ParametersListUtil.join(it.defaultArguments))
                programParametersEditor.defaultValue.set(ParametersListUtil.join(it.defaultArguments))
            } else {
                programParametersEditor.parametersString.set("")
                programParametersEditor.defaultValue.set("")
            }
            workingDirectorySelector.defaultValue.set(it.workingDirectory)
        }
    }

    private fun recalculateTrackProjectOutput() {
        val selectedProject = projectSelector.project.valueOrNull
        val selectedTfm = tfmSelector.string.valueOrNull
        if (selectedProject != null && selectedTfm != null) {
            selectedProject.projectOutputs.singleOrNull { it.tfm == selectedTfm }?.let {
                trackProjectExePath = exePathSelector.path.value == it.exePath
                trackProjectArguments = (it.defaultArguments.isEmpty()
                        || programParametersEditor.parametersString.value ==
                        ParametersListUtil.join(it.defaultArguments))
                trackProjectWorkingDirectory = workingDirectorySelector.path.value == it.workingDirectory
            }

            val result = portRegex.find(programParametersEditor.parametersString.value)
            if (result != null && result.groups.count() == 2) {
                urlEditor.defaultValue.value = "http://localhost:${result.groupValues[1]}"
                urlEditor.text.value = "http://localhost:${result.groupValues[1]}"
                dotNetBrowserSettingsEditor.settings.value = BrowserSettings(false, false, null)
            }
        }
    }

    private fun handleProjectSelection(project: RunnableProject) {
        if (!isLoaded) return
        reloadTfmSelector(project)

        val startBrowserUrl = project.customAttributes.singleOrNull { it.key == Key.StartBrowserUrl }?.value ?: ""
        val launchBrowser = project.customAttributes.singleOrNull { it.key == Key.LaunchBrowser }?.value?.toBoolean() ?: false
        if (startBrowserUrl.isNotEmpty()) {
            urlEditor.defaultValue.value = startBrowserUrl
            urlEditor.text.value = startBrowserUrl
            dotNetBrowserSettingsEditor.settings.value = BrowserSettings(launchBrowser, false, null)
        }

        environmentVariablesEditor.envs.set(project.environmentVariables.map { it.key to it.value }.toMap())
    }

    private fun reloadTfmSelector(project: RunnableProject) {
        tfmSelector.stringList.clear()
        project.projectOutputs.map { it.tfm }.sorted().forEach {
            tfmSelector.stringList.add(it)
        }
        if (tfmSelector.stringList.isNotEmpty()) {
            tfmSelector.string.set(tfmSelector.stringList.first())
        }
        handleChangeTfmSelection()
    }

    fun reset(projectFilePath: String,
              trackProjectExePath: Boolean,
              trackProjectArguments: Boolean,
              trackProjectWorkingDirectory: Boolean,
              projectTfm: String,
              exePath: String,
              programParameters: String,
              workingDirectory: String,
              envs: Map<String, String>,
              passParentEnvs: Boolean,
              useExternalConsole: Boolean,
              isUnloadedProject: Boolean,
              dotNetStartBrowserParameters: DotNetStartBrowserParameters) {
        fun resetProperties(exePath: String, programParameters: String, workingDirectory: String) {
            super.reset(
                    exePath,
                    programParameters,
                    workingDirectory,
                    envs,
                    passParentEnvs,
                    false,
                    "",
                    useExternalConsole
            )
        }

        isLoaded = false
        this.trackProjectExePath = trackProjectExePath
        this.trackProjectArguments = trackProjectArguments
        this.trackProjectWorkingDirectory = trackProjectWorkingDirectory
        runnableProjectsModel.projects.adviseOnce(lifetime) { projectList ->
            val mappedProjectList = projectList.map(AzureFunctionsRunnableProjectUtil::patchRunnableProjectOutputs).toList()

            urlEditor.defaultValue.value = dotNetStartBrowserParameters.url
            urlEditor.text.value = dotNetStartBrowserParameters.url
            dotNetBrowserSettingsEditor.settings.set(BrowserSettings(
                    dotNetStartBrowserParameters.startAfterLaunch,
                    dotNetStartBrowserParameters.withJavaScriptDebugger,
                    dotNetStartBrowserParameters.browser))

            if (projectFilePath.isEmpty() || mappedProjectList.none {
                        it.projectFilePath == projectFilePath && AzureFunctionsHostConfigurationType.isTypeApplicable(it.kind)
                    }) {
                // Case when project didn't selected otherwise we should generate fake project to avoid drop user settings.
                if (projectFilePath.isEmpty() || !isUnloadedProject) {
                    mappedProjectList.firstOrNull { type.isApplicable(it.kind) }?.let { project ->
                        projectSelector.project.set(project)
                        isLoaded = true
                        handleProjectSelection(project)
                    }
                } else {
                    val fakeProjectName = File(projectFilePath).name
                    val fakeProject = RunnableProject(
                            fakeProjectName, fakeProjectName, projectFilePath, RunnableProjectKind.Unloaded,
                            listOf(ProjectOutput(projectTfm, exePath, ParametersListUtil.parse(programParameters), workingDirectory, "")),
                            envs.map { EnvironmentVariable(it.key, it.value) }.toList(), null, listOf()
                    )
                    projectSelector.projectList.apply {
                        clear()
                        addAll(mappedProjectList + fakeProject)
                    }
                    projectSelector.project.set(fakeProject)
                    reloadTfmSelector(fakeProject)
                    resetProperties(exePath, programParameters, workingDirectory)
                }
            } else {
                mappedProjectList.singleOrNull {
                    it.projectFilePath == projectFilePath && AzureFunctionsHostConfigurationType.isTypeApplicable(it.kind)
                }?.let { project ->
                    projectSelector.project.set(project)
                    reloadTfmSelector(project)

                    val projectTfmExists = project.projectOutputs.any { it.tfm == projectTfm }
                    val selectedTfm = if (projectTfmExists) projectTfm else project.projectOutputs.firstOrNull()?.tfm ?: ""
                    tfmSelector.string.set(selectedTfm)
                    val projectOutput = project.projectOutputs.singleOrNull { it.tfm == selectedTfm }
                    val effectiveExePath = if (trackProjectExePath && projectOutput != null) projectOutput.exePath else exePath
                    val effectiveProgramParameters =
                            if (trackProjectArguments && projectOutput != null && projectOutput.defaultArguments.isNotEmpty())
                                ParametersListUtil.join(projectOutput.defaultArguments).replace("\\\"", "\"") else programParameters
                    programParametersEditor.defaultValue.set(effectiveProgramParameters)
                    val effectiveWorkingDirectory = if (trackProjectWorkingDirectory && projectOutput != null)
                        projectOutput.workingDirectory else workingDirectory
                    resetProperties(effectiveExePath, effectiveProgramParameters, effectiveWorkingDirectory)
                }
            }
            isLoaded = true
        }
    }
}