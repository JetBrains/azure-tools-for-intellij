/*
 * Copyright 2018-2025 JetBrains s.r.o. and contributors. Use of this source code is governed by the MIT license.
 */

@file:Suppress("UnstableApiUsage")

package com.microsoft.azure.toolkit.intellij.appservice.docker.debug

import com.intellij.openapi.application.EDT
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.diagnostic.trace
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.SystemInfo
import com.intellij.platform.ide.progress.withBackgroundProgress
import com.intellij.platform.util.progress.forEachWithProgress
import com.jetbrains.rd.platform.util.idea.LifetimedService
import com.jetbrains.rider.BundledAssemblyLauncherInfo
import com.jetbrains.rider.CPUKind
import com.jetbrains.rider.PathInfo
import com.jetbrains.rider.debugger.RiderDebugRunner
import com.jetbrains.rider.debugger.RiderDebuggerBundle
import com.jetbrains.rider.debugger.attach.remoting.RemoteDebuggerToolsDownloadHelper
import com.jetbrains.rider.debugger.attach.remoting.tools.local.DefaultLocalDebuggerTools
import com.jetbrains.rider.environment.local.RiderLocalEnvironment
import com.jetbrains.rider.ijent.extensions.toRd
import com.jetbrains.rider.launchers.DEBUGGER_WORKER_LAUNCHER
import com.jetbrains.rider.model.RdProjectDebugModePropertiesResponse
import com.jetbrains.rider.model.RdProjectPropertiesRequest
import com.jetbrains.rider.model.dockerModel
import com.jetbrains.rider.plugins.appender.docker.common.DockerContainerType
import com.jetbrains.rider.plugins.appender.docker.debug.providers.RiderDockerDebugProvider
import com.jetbrains.rider.plugins.appender.docker.deployment.RiderDockerDeploymentFromFileModel
import com.jetbrains.rider.plugins.appender.docker.deployment.RiderDockerDeploymentModel
import com.jetbrains.rider.plugins.appender.docker.deployment.RiderDockerDeploymentTransformer.RiderDockerDeploymentParameters
import com.jetbrains.rider.plugins.appender.docker.deployment.TransformedDeploymentEnvironmentVariable
import com.jetbrains.rider.plugins.appender.docker.deployment.TransformedDeploymentVolume
import com.jetbrains.rider.plugins.appender.docker.fileSharing.DockerMacOsFileSharingService
import com.jetbrains.rider.plugins.appender.docker.settings.RiderDockerSettings
import com.jetbrains.rider.projectView.solution
import com.jetbrains.rider.run.dotNetCore.toWindowsCPUKind
import com.microsoft.azure.toolkit.intellij.legacy.function.coreTools.FunctionCoreToolsManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.nio.file.Files
import java.nio.file.Path
import kotlin.coroutines.cancellation.CancellationException
import kotlin.io.path.absolute
import kotlin.io.path.absolutePathString
import kotlin.io.path.exists

/**
 * Prepares Docker debug mode for Azure Functions Projects.
 *
 * 1. Creates a temporary artifacts directory with a JSON file where `func start`
 * will later write the Functions worker PID.
 * 2. Downloads Azure Functions Core Tools
 * and Rider debugger tools and mounts them into the container
 */
@Service(Service.Level.PROJECT)
internal class DockerDebugModeAzureService(private val project: Project) : LifetimedService() {
    companion object {
        private val LOG = logger<DockerDebugModeAzureService>()

        private const val AZURE_FUNCTIONS_RUNTIME_VERSION = "v4"
        private const val WINDOWS_AZURE_FUNCTIONS_TOOLS_FOLDER = "C:\\AzureFunctionsCoreTools"
        internal const val LINUX_AZURE_FUNCTIONS_TOOLS_FOLDER = "/opt/AzureFunctionsCoreTools"

        private const val AZURE_FUNCTIONS_JSON_OUTPUT_FILE_NAME = "functions-output.json"
        private const val AZURE_FUNCTIONS_ARTIFACTS_DIR_PREFIX = "rider-azure-functions-"

        private const val WINDOWS_AZURE_FUNCTIONS_ARTIFACTS_FOLDER = "C:\\RiderAzureFunctions"
        private const val WINDOWS_AZURE_FUNCTIONS_JSON_OUTPUT_FILE =
            "$WINDOWS_AZURE_FUNCTIONS_ARTIFACTS_FOLDER\\$AZURE_FUNCTIONS_JSON_OUTPUT_FILE_NAME"
        private const val LINUX_AZURE_FUNCTIONS_ARTIFACTS_FOLDER = "/var/opt/JetBrains/RiderAzureFunctions"
        private const val LINUX_AZURE_FUNCTIONS_JSON_OUTPUT_FILE =
            "$LINUX_AZURE_FUNCTIONS_ARTIFACTS_FOLDER/$AZURE_FUNCTIONS_JSON_OUTPUT_FILE_NAME"

        private const val WINDOWS_AZURE_FUNCTIONS_FUNC_PATH =
            "$WINDOWS_AZURE_FUNCTIONS_TOOLS_FOLDER\\func.exe"
        private const val LINUX_AZURE_FUNCTIONS_FUNC_PATH =
            "$LINUX_AZURE_FUNCTIONS_TOOLS_FOLDER/func"

        private const val WINDOWS_DEBUGGER_WORKER_FOLDER = "C:\\RiderDebugger"
        private const val WINDOWS_DEBUGGER_WORKER_LOG_FOLDER = "C:\\RiderDebuggerLogs"
        private const val WINDOWS_DEBUGGER_WORKER_LOG_CONF_FOLDER = "C:\\RiderDebuggerLogConfig"
        private const val WINDOWS_DEBUGGER_WORKER_LOG_CONF_FILE = "C:\\RiderDebuggerLogConfig\\backend-log.xml"
        private const val LINUX_DEBUGGER_WORKER_FOLDER = "/opt/JetBrains/RiderDebuggerTools"
        private const val LINUX_DEBUGGER_WORKER_LOG_FOLDER = "/var/opt/JetBrains/RiderDebuggerTools"
        private const val LINUX_DEBUGGER_WORKER_LOG_CONF_FILE = "/etc/opt/JetBrains/RiderDebuggerTools/backend-log.xml"
        private const val BUILD_CONFIGURATION_ARG = "BUILD_CONFIGURATION"
        private const val BUILD_CONFIGURATION_DEBUG_VALUE = "Debug"

        private const val AZURE_FUNCTIONS_JOB_HOST_LOGGING = "AzureFunctionsJobHost__Logging__Console__IsEnabled"

        internal fun getInstance(project: Project): DockerDebugModeAzureService = project.service()
    }

    internal suspend fun prepareDebugMode(
        deploymentParams: RiderDockerDeploymentParameters,
    ): DockerDebugModeAzureInfo? {
        LOG.trace("Preparing Azure Functions Debug mode for Dockerfile deployment")

        val debuggerWorkerLogDir = RiderDebugRunner.createNextDebuggerLogsSubDir(project)

        return getDebugModeInfo(
            deploymentParams = deploymentParams,
            debuggerWorkerLogDir = debuggerWorkerLogDir,
            hostPortsInUse = mutableSetOf(),
        )
    }

    internal suspend fun prepareDebugMode(
        deploymentParams: Map<String, RiderDockerDeploymentParameters>,
    ): Map<String, DockerDebugModeAzureInfo> {
        LOG.trace("Preparing Azure Functions Debug mode for Docker Compose deployment")

        val debugModeInfos = mutableMapOf<String, DockerDebugModeAzureInfo>()
        val debuggerWorkerLogDir = RiderDebugRunner.createNextDebuggerLogsSubDir(project)
        val usedPorts = mutableSetOf<Int>()

        deploymentParams.entries.forEachWithProgress { (service, serviceDeploymentParams) ->
            val debugModeInfo = getDebugModeInfo(
                deploymentParams = serviceDeploymentParams,
                debuggerWorkerLogDir = debuggerWorkerLogDir,
                hostPortsInUse = usedPorts,
            ) ?: return@forEachWithProgress

            debugModeInfos[service] = debugModeInfo
        }

        return debugModeInfos
    }

    private suspend fun getDebugModeInfo(
        deploymentParams: RiderDockerDeploymentParameters,
        debuggerWorkerLogDir: Path,
        hostPortsInUse: MutableSet<Int>,
    ): DockerDebugModeAzureInfo? {
        val projectProperties = getProjectDebugModeProperties(deploymentParams)
        LOG.trace { "Project debug mode properties: $projectProperties" }

        val dockerDeploymentModel = deploymentParams.deploymentModel

        val debugProvider = RiderDockerDebugProvider.EP_NAME.extensionList
            .firstOrNull { it.debuggerKey == DockerDebugModeAzureProvider.DEBUGGER_KEY }

        if (debugProvider == null) {
            LOG.warn("Unable to obtain applicable debug provider for Azure Functions")
            return null
        }

        val containerOs = dockerDeploymentModel.getContainerOs()
        val isWindowsContainer = containerOs == DockerContainerType.Windows
        val cpuKind = dockerDeploymentModel.getCpuKind()
        val containerDotnetVersion = dockerDeploymentModel.getImageProperties()?.dotnetProperties?.dotNetVersion

        val debuggerToolsDirectory =
            withBackgroundProgress(
                project,
                RiderDebuggerBundle.message("rider.debug.downloading.debugger.tools.message")
            ) {
                val tools = DefaultLocalDebuggerTools(project, cpuKind)
                try {
                    RemoteDebuggerToolsDownloadHelper.downloadAndUnarchive(tools, project)?.toPath()
                } catch (ce: CancellationException) {
                    throw ce
                } catch (e: Exception) {
                    LOG.warn("Unable to download remote debugger tools", e)
                    null
                }
            }

        if (debuggerToolsDirectory == null || !debuggerToolsDirectory.exists()) {
            LOG.warn("Unable to download debugger tools for Azure Functions")
            return null
        }

        val settings = RiderDockerSettings.getInstance()
        val debuggerTimeout = settings.debuggerTimeout
        val useLoopbackAddress = settings.useLoopbackAddress

        val containerCpuKind = if (isWindowsContainer) CPUKind.Win64 else CPUKind.Linux64
        val azureFunctionsToolsDir = FunctionCoreToolsManager.getInstance().downloadLatestFunctionCoreToolsForVersion(
            AZURE_FUNCTIONS_RUNTIME_VERSION,
            containerCpuKind
        )

        if (azureFunctionsToolsDir == null || !azureFunctionsToolsDir.exists()) {
            LOG.warn("Unable to download Azure Functions Core Tools for $containerCpuKind")
            return null
        }

        val azureFunctionsArtifactsHostDir = createAzureFunctionsArtifactsDirectory()

        val debuggerPorts = getDebuggerPorts(dockerDeploymentModel, hostPortsInUse)
        val debuggerVolumes = getDebuggerVolumes(
            debuggerToolsDirectory,
            debuggerWorkerLogDir,
            azureFunctionsToolsDir,
            azureFunctionsArtifactsHostDir,
            isWindowsContainer
        )
        val environmentVariables = getDebuggerEnvironmentVariables(isWindowsContainer)
        val buildArgs = getDebuggerBuildArgs()

        val targetCpuKind = calculateTargetCpu(isWindowsContainer, cpuKind, projectProperties)
        val debuggerDirectoryInsideContainer =
            if (isWindowsContainer) WINDOWS_DEBUGGER_WORKER_FOLDER else LINUX_DEBUGGER_WORKER_FOLDER

        val launcherInfo: BundledAssemblyLauncherInfo =
            DEBUGGER_WORKER_LAUNCHER.prepareForRemoteExecution(
                PathInfo(
                    debuggerDirectoryInsideContainer,
                    targetCpuKind
                )
            )

        val azureFunctionsToolsPathInContainer =
            if (isWindowsContainer) WINDOWS_AZURE_FUNCTIONS_FUNC_PATH
            else LINUX_AZURE_FUNCTIONS_FUNC_PATH

        val azureFunctionsJsonOutputFileInContainer =
            if (isWindowsContainer) WINDOWS_AZURE_FUNCTIONS_JSON_OUTPUT_FILE
            else LINUX_AZURE_FUNCTIONS_JSON_OUTPUT_FILE

        val azureFunctionsJsonOutputFileInHost =
            azureFunctionsArtifactsHostDir.resolve(AZURE_FUNCTIONS_JSON_OUTPUT_FILE_NAME).toAbsolutePath().toString()

        val debugModeInfo = DockerDebugModeAzureInfo(
            debugProvider = debugProvider,
            debuggerLauncherInfo = launcherInfo,
            targetCpuKind = targetCpuKind,
            debuggerPorts = debuggerPorts,
            useLoopbackAddress = useLoopbackAddress,
            debuggerVolumes = debuggerVolumes,
            environmentVariables = environmentVariables,
            buildArgs = buildArgs,
            containerOs = containerOs,
            containerDotnetVersion = containerDotnetVersion,
            debuggerTimeout = debuggerTimeout,
            azureFunctionsToolsPathInContainer = azureFunctionsToolsPathInContainer,
            azureFunctionsJsonOutputFileInContainer = azureFunctionsJsonOutputFileInContainer,
            azureFunctionsJsonOutputFileInHost = azureFunctionsJsonOutputFileInHost,
        )

        LOG.trace { "Calculated Azure Functions Debug mode info: $debugModeInfo" }
        return debugModeInfo
    }

    private suspend fun getProjectDebugModeProperties(
        deploymentParams: RiderDockerDeploymentParameters,
    ): RdProjectDebugModePropertiesResponse? {
        if (deploymentParams.deploymentModel !is RiderDockerDeploymentFromFileModel) return null

        if (deploymentParams.projectFilePath == null) {
            LOG.trace { "Unable to get project path for ${(deploymentParams.deploymentModel as RiderDockerDeploymentFromFileModel).dockerfilePath}" }
            return null
        }

        return withContext(Dispatchers.EDT) {
            val request = RdProjectPropertiesRequest(deploymentParams.projectFilePath!!.absolute().toRd())
            project.solution.dockerModel.getProjectDebugModeProperties.startSuspending(request)
        }
    }

    private suspend fun getDebuggerPorts(
        dockerDeploymentModel: RiderDockerDeploymentModel,
        hostPortsInUse: MutableSet<Int>,
    ): DockerDebuggerPorts {
        val containerPortsInUse = dockerDeploymentModel.getContainerPorts()
        val debuggerPorts = getAvailableDebuggerPorts(containerPortsInUse, hostPortsInUse)
        hostPortsInUse.add(debuggerPorts.frontendPorts.hostPort)
        hostPortsInUse.add(debuggerPorts.backendPorts.hostPort)
        hostPortsInUse.add(debuggerPorts.roslynWorkerPorts.hostPort)
        return debuggerPorts
    }

    private suspend fun getDebuggerVolumes(
        debuggerToolsDirectory: Path,
        debuggerWorkerLogDir: Path,
        azureFunctionsToolsDir: Path,
        azureFunctionsArtifactsHostDir: Path,
        isWindowsContainer: Boolean,
    ): DockerDebuggerVolumes {
        val debuggerWorkerLogConfFile = RiderLocalEnvironment.backendLogXmlPath

        return if (isWindowsContainer) {
            getDebuggerVolumesForWindowsContainer(
                debuggerToolsDirectory,
                debuggerWorkerLogConfFile,
                debuggerWorkerLogDir,
                azureFunctionsToolsDir,
                azureFunctionsArtifactsHostDir
            )
        } else if (SystemInfo.isMac) {
            getDebuggerVolumesForLinuxContainerOnMacOs(
                debuggerToolsDirectory,
                debuggerWorkerLogConfFile,
                debuggerWorkerLogDir,
                azureFunctionsToolsDir,
                azureFunctionsArtifactsHostDir
            )
        } else {
            getDebuggerVolumesForLinuxContainer(
                debuggerToolsDirectory,
                debuggerWorkerLogConfFile,
                debuggerWorkerLogDir,
                azureFunctionsToolsDir,
                azureFunctionsArtifactsHostDir
            )
        }
    }

    private fun getDebuggerVolumesForWindowsContainer(
        debuggerToolsDirectory: Path,
        debuggerWorkerLogConfFile: Path,
        debuggerWorkerLogDir: Path,
        azureFunctionsToolsDir: Path,
        azureFunctionsArtifactsHostDir: Path,
    ): DockerDebuggerVolumes {
        val workerFolder = TransformedDeploymentVolume(
            WINDOWS_DEBUGGER_WORKER_FOLDER,
            debuggerToolsDirectory.absolutePathString(),
            true
        )

        val logConfigFileAvailable = debuggerWorkerLogConfFile.exists()
        val logConfigFolder =
            if (logConfigFileAvailable)
                TransformedDeploymentVolume(
                    WINDOWS_DEBUGGER_WORKER_LOG_CONF_FOLDER,
                    debuggerWorkerLogConfFile.parent.absolutePathString(),
                    true
                )
            else null

        val logFolderAvailable = debuggerWorkerLogDir.exists()
        val logFolder =
            if (logFolderAvailable)
                TransformedDeploymentVolume(
                    WINDOWS_DEBUGGER_WORKER_LOG_FOLDER,
                    debuggerWorkerLogDir.absolutePathString(),
                    false
                )
            else null

        val azureFunctionsToolsFolder =
            TransformedDeploymentVolume(
                WINDOWS_AZURE_FUNCTIONS_TOOLS_FOLDER,
                azureFunctionsToolsDir.absolutePathString(),
                true
            )

        val azureFunctionsArtifactsFolder =
            TransformedDeploymentVolume(
                WINDOWS_AZURE_FUNCTIONS_ARTIFACTS_FOLDER,
                azureFunctionsArtifactsHostDir.absolutePathString(),
                false
            )

        return DockerDebuggerVolumes(
            workerFolder,
            logConfigFolder,
            logFolder,
            azureFunctionsToolsFolder,
            azureFunctionsArtifactsFolder
        )
    }

    private suspend fun getDebuggerVolumesForLinuxContainerOnMacOs(
        debuggerToolsDirectory: Path,
        debuggerWorkerLogConfFile: Path,
        debuggerWorkerLogDir: Path,
        azureFunctionsToolsDir: Path,
        azureFunctionsArtifactsHostDir: Path,
    ): DockerDebuggerVolumes {
        val fileSharingService = DockerMacOsFileSharingService.getInstance()

        val workerFolder =
            TransformedDeploymentVolume(LINUX_DEBUGGER_WORKER_FOLDER, debuggerToolsDirectory.absolutePathString(), true)

        val logConfigFileAvailable =
            debuggerWorkerLogConfFile.exists() && fileSharingService.isPathAvailable(debuggerWorkerLogConfFile) != false
        val logConfigFile =
            if (logConfigFileAvailable)
                TransformedDeploymentVolume(
                    LINUX_DEBUGGER_WORKER_LOG_CONF_FILE,
                    debuggerWorkerLogConfFile.absolutePathString(),
                    true
                )
            else null

        val logFolderAvailable =
            debuggerWorkerLogDir.exists() && fileSharingService.isPathAvailable(debuggerWorkerLogDir) != false
        val logFolder =
            if (logFolderAvailable)
                TransformedDeploymentVolume(
                    LINUX_DEBUGGER_WORKER_LOG_FOLDER,
                    debuggerWorkerLogDir.absolutePathString(),
                    false
                )
            else null

        val azureFunctionsToolsFolder = TransformedDeploymentVolume(
            LINUX_AZURE_FUNCTIONS_TOOLS_FOLDER,
            azureFunctionsToolsDir.absolutePathString(),
            true
        )

        val azureFunctionsArtifactsFolder = TransformedDeploymentVolume(
            LINUX_AZURE_FUNCTIONS_ARTIFACTS_FOLDER,
            azureFunctionsArtifactsHostDir.absolutePathString(),
            false
        )

        return DockerDebuggerVolumes(
            workerFolder,
            logConfigFile,
            logFolder,
            azureFunctionsToolsFolder,
            azureFunctionsArtifactsFolder
        )
    }

    private fun getDebuggerVolumesForLinuxContainer(
        debuggerToolsDirectory: Path,
        debuggerWorkerLogConfFile: Path,
        debuggerWorkerLogDir: Path,
        azureFunctionsToolsDir: Path,
        azureFunctionsArtifactsHostDir: Path,
    ): DockerDebuggerVolumes {
        val workerFolder =
            TransformedDeploymentVolume(LINUX_DEBUGGER_WORKER_FOLDER, debuggerToolsDirectory.absolutePathString(), true)

        val logConfigFileAvailable = debuggerWorkerLogConfFile.exists()
        val logConfigFile =
            if (logConfigFileAvailable)
                TransformedDeploymentVolume(
                    LINUX_DEBUGGER_WORKER_LOG_CONF_FILE,
                    debuggerWorkerLogConfFile.absolutePathString(),
                    true
                )
            else null

        val logFolderAvailable = debuggerWorkerLogDir.exists()
        val logFolder =
            if (logFolderAvailable)
                TransformedDeploymentVolume(
                    LINUX_DEBUGGER_WORKER_LOG_FOLDER,
                    debuggerWorkerLogDir.absolutePathString(),
                    false
                )
            else null

        val azureFunctionsTools =
            TransformedDeploymentVolume(
                LINUX_AZURE_FUNCTIONS_TOOLS_FOLDER,
                azureFunctionsToolsDir.absolutePathString(),
                true
            )

        val azureFunctionsArtifactsFolder =
            TransformedDeploymentVolume(
                LINUX_AZURE_FUNCTIONS_ARTIFACTS_FOLDER,
                azureFunctionsArtifactsHostDir.absolutePathString(),
                false
            )

        return DockerDebuggerVolumes(
            workerFolder,
            logConfigFile,
            logFolder,
            azureFunctionsTools,
            azureFunctionsArtifactsFolder
        )
    }

    private fun getDebuggerEnvironmentVariables(isWindowsContainer: Boolean): DockerDebuggerAzureEnvironmentVariables {
        val logFolderVar: TransformedDeploymentEnvironmentVariable
        val logConfVar: TransformedDeploymentEnvironmentVariable

        if (isWindowsContainer) {
            logFolderVar = TransformedDeploymentEnvironmentVariable(
                RiderDebugRunner.DEBUGGER_WORKER_LOG_DIR_ENV_KEY,
                WINDOWS_DEBUGGER_WORKER_LOG_FOLDER
            )
            logConfVar = TransformedDeploymentEnvironmentVariable(
                RiderDebugRunner.DEBUGGER_WORKER_LOG_CONF_ENV_KEY,
                WINDOWS_DEBUGGER_WORKER_LOG_CONF_FILE
            )
        } else {
            logFolderVar = TransformedDeploymentEnvironmentVariable(
                RiderDebugRunner.DEBUGGER_WORKER_LOG_DIR_ENV_KEY,
                LINUX_DEBUGGER_WORKER_LOG_FOLDER
            )
            logConfVar = TransformedDeploymentEnvironmentVariable(
                RiderDebugRunner.DEBUGGER_WORKER_LOG_CONF_ENV_KEY,
                LINUX_DEBUGGER_WORKER_LOG_CONF_FILE
            )
        }

        val azureConsoleLoggingVar = TransformedDeploymentEnvironmentVariable(AZURE_FUNCTIONS_JOB_HOST_LOGGING, "true")

        return DockerDebuggerAzureEnvironmentVariables(
            logFolderVariable = logFolderVar,
            logConfigFolderVariable = logConfVar,
            azureFunctionsConsoleLoggingEnabled = azureConsoleLoggingVar
        )
    }

    private fun getDebuggerBuildArgs(): DockerDebuggerBuildArgs {
        return DockerDebuggerBuildArgs(
            TransformedDeploymentEnvironmentVariable(BUILD_CONFIGURATION_ARG, BUILD_CONFIGURATION_DEBUG_VALUE)
        )
    }

    private fun calculateTargetCpu(
        isWindowsContainer: Boolean,
        cpuKind: CPUKind,
        projectProperties: RdProjectDebugModePropertiesResponse?,
    ): CPUKind {
        if (!isWindowsContainer || projectProperties == null) return cpuKind
        return projectProperties.dotnetAssemblyDetails.pePlatform.toWindowsCPUKind(cpuKind.cpuArch)
    }

    private fun createAzureFunctionsArtifactsDirectory(): Path {
        val dir = Files.createTempDirectory(AZURE_FUNCTIONS_ARTIFACTS_DIR_PREFIX)
        dir.toFile().deleteOnExit()
        return dir
    }
}