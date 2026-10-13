package com.microsoft.azure.toolkit.intellij.bicep.lsp

import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.execution.process.CapturingProcessHandler
import com.intellij.openapi.application.ModalityState
import com.intellij.openapi.application.asContextElement
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.rethrowControlFlowException
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.project.Project
import com.jetbrains.rider.environment.initializeAndGetEnvironment
import com.microsoft.azure.toolkit.intellij.bicep.BicepBundle
import com.microsoft.azure.toolkit.intellij.bicep.settings.BicepSettings
import com.microsoft.azure.toolkit.intellij.bicep.settings.BicepSettingsState.LanguageServerSource
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.nio.file.Path
import java.util.concurrent.atomic.AtomicReference
import kotlin.io.path.absolutePathString
import kotlin.io.path.exists
import kotlin.io.path.isRegularFile

@Service(Service.Level.PROJECT)
internal class BicepSchedulingService(private val project: Project, val coroutineScope: CoroutineScope) {
    companion object {
        fun getInstance(project: Project): BicepSchedulingService = project.service<BicepSchedulingService>()
    }

    private val setupInProgress = MutableStateFlow(false)
    private val lsVerified = AtomicReference(LsValidity.YET_UNKNOWN)

    val isLsSetupInProgress: StateFlow<Boolean>
        get() = setupInProgress

    val currentLsInfrastructureValidity: LsValidity
        get() = lsVerified.get()

    fun scheduleLsDownload() {
        scheduleLsInfrastructureSetup(LsSetupMode.DOWNLOAD_AND_VALIDATE)
    }

    fun scheduleLsValidation() {
        scheduleLsInfrastructureSetup(LsSetupMode.VALIDATE_EXISTING)
    }

    private fun scheduleLsInfrastructureSetup(setupMode: LsSetupMode) {
        if (!setupInProgress.compareAndSet(expect = false, update = true)) return

        lsVerified.set(LsValidity.YET_UNKNOWN)
        coroutineScope.launch(Dispatchers.Default + ModalityState.any().asContextElement()) {
            try {
                val languageServerSource = BicepSettings.getInstance(project).getLanguageServerSource()
                val infrastructureValidity = try {
                    ensureInfrastructureValid(setupMode, languageServerSource)
                } catch (exception: Exception) {
                    rethrowControlFlowException(exception)
                    thisLogger().warn("Unable to validate Bicep language server infrastructure", exception)
                    LsValidity.INVALID
                }
                lsVerified.set(infrastructureValidity)
                if (infrastructureValidity == LsValidity.VALID) {
                    forceRunLanguageServer(project)
                }
                displayAvailabilityHint(infrastructureValidity, setupMode)

                if (infrastructureValidity == LsValidity.VALID
                    && setupMode == LsSetupMode.VALIDATE_EXISTING
                    && languageServerSource == LanguageServerSource.LATEST
                ) {
                    BicepInfrastructureLoader.getInstance().downloadLatestLsInfrastructure(project)
                }
            } finally {
                setupInProgress.compareAndSet(expect = true, update = false)
                reloadEditorNotifications(project)
            }
        }
    }

    private enum class LsSetupMode {
        VALIDATE_EXISTING, DOWNLOAD_AND_VALIDATE
    }

    private suspend fun ensureInfrastructureValid(
        setupMode: LsSetupMode,
        languageServerSource: LanguageServerSource
    ): LsValidity {
        val settings = BicepSettings.getInstance(project)
        val infrastructureService = BicepInfrastructureService.getInstance(project)
        val bicepLS = when (languageServerSource) {
            LanguageServerSource.LOCAL_FILE -> null

            LanguageServerSource.SPECIFIC_VERSION -> {
                val version = settings.getLanguageServerVersion()?.takeIf { it.isNotBlank() }
                    ?: return LsValidity.INVALID
                BicepLS(version)
            }

            LanguageServerSource.LATEST -> {
                val version = when (setupMode) {
                    LsSetupMode.VALIDATE_EXISTING -> infrastructureService.findLatestVersion()
                    LsSetupMode.DOWNLOAD_AND_VALIDATE -> BicepInfrastructureLoader.getInstance().findLatestVersion()
                } ?: return LsValidity.INVALID
                BicepLS(version)
            }
        }

        val executablePath = if (bicepLS != null) {
            if (!ensureInfrastructureExists(setupMode, bicepLS) || !infrastructureService.isValid(bicepLS)) {
                return LsValidity.INVALID
            }
            infrastructureService.findExecutablePath(bicepLS)
        } else {
            val path = settings.getLanguageServerPath()
                ?.takeIf { it.isNotBlank() }
                ?: return LsValidity.INVALID
            Path.of(path)
        }

        return if (tryRunServerAndCheckNoErrorsInOutput(executablePath)) {
            LsValidity.VALID
        } else {
            LsValidity.INVALID
        }
    }

    private suspend fun ensureInfrastructureExists(setupMode: LsSetupMode, bicepLS: BicepLS): Boolean {
        return when (setupMode) {
            LsSetupMode.DOWNLOAD_AND_VALIDATE ->
                BicepInfrastructureLoader.getInstance().downloadLsInfrastructure(project, bicepLS)

            LsSetupMode.VALIDATE_EXISTING ->
                BicepInfrastructureService.getInstance(project).findExecutablePath(bicepLS).exists()
        }
    }

    private suspend fun tryRunServerAndCheckNoErrorsInOutput(executablePath: Path): Boolean {
        if (!executablePath.isRegularFile()) return false

        val riderEnvironment = project.initializeAndGetEnvironment()
        val dotnetExecutablePath = riderEnvironment.getRuntime().cliPath().absolutePathString()
        val testCommandLine = GeneralCommandLine(dotnetExecutablePath, executablePath.absolutePathString())
        val testOutput = withContext(Dispatchers.IO) {
            CapturingProcessHandler.Silent(testCommandLine).runProcess(1000, true)
        }
        if (testOutput.stderr.isNotBlank()) {
            thisLogger().warn("Bicep language server output stderr: ${testOutput.stderr}")
            return false
        }

        return true
    }

    private suspend fun displayAvailabilityHint(lsValidity: LsValidity, setupMode: LsSetupMode) {
        when (lsValidity) {
            LsValidity.VALID -> {
                displayPopupWithServerInfo(project, BicepBundle.message("hint.ls.features.available"))
            }

            LsValidity.INVALID if setupMode == LsSetupMode.DOWNLOAD_AND_VALIDATE -> {
                displayPopupWithServerInfo(project, BicepBundle.message("hint.ls.installation.error"))
            }

            else -> {}
        }
    }
}

internal enum class LsValidity {
    VALID, INVALID, YET_UNKNOWN
}
