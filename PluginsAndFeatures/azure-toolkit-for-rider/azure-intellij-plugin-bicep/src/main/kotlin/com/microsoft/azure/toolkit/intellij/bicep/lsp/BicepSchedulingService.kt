package com.microsoft.azure.toolkit.intellij.bicep.lsp

import com.intellij.execution.process.CapturingProcessHandler
import com.intellij.openapi.application.ModalityState
import com.intellij.openapi.application.asContextElement
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.project.Project
import com.jetbrains.rider.environment.initializeAndGetEnvironment
import com.microsoft.azure.toolkit.intellij.bicep.BicepBundle
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.atomic.AtomicReference

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
        cleanTmpDirectorySafe(BicepLS)
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
                val infrastructureValidity = ensureInfrastructureValid(setupMode)
                lsVerified.set(infrastructureValidity)
                if (infrastructureValidity == LsValidity.VALID) {
                    forceRunLanguageServer(project)
                }
                displayAvailabilityHint(infrastructureValidity, setupMode)
            } finally {
                setupInProgress.compareAndSet(expect = true, update = false)
                reloadEditorNotifications(project)
            }
        }
    }

    private enum class LsSetupMode {
        VALIDATE_EXISTING, DOWNLOAD_AND_VALIDATE
    }

    private suspend fun ensureInfrastructureValid(setupMode: LsSetupMode): LsValidity {
        return if (ensureInfrastructureExists(setupMode) && isInfrastructureValid() && tryRunServerAndCheckNoErrorsInOutput())
            LsValidity.VALID
        else
            LsValidity.INVALID
    }

    private suspend fun ensureInfrastructureExists(setupMode: LsSetupMode): Boolean {
        return when (setupMode) {
            LsSetupMode.DOWNLOAD_AND_VALIDATE -> downloadLsInfrastructure(project)
            LsSetupMode.VALIDATE_EXISTING -> isInfrastructurePresent()
        }
    }

    private fun isInfrastructureValid(): Boolean {
        return LsInfrastructure.allKnown().all(LsInfrastructure::isValid)
    }

    private suspend fun tryRunServerAndCheckNoErrorsInOutput(): Boolean {
        val environment = project.initializeAndGetEnvironment()
        val testCommandLine = prepareBicepServerLaunchCommandLine(environment)
        val testOutput = withContext(Dispatchers.IO) {
            CapturingProcessHandler.Silent(testCommandLine).runProcess(1000, true)
        }
        if (testOutput.stderr.isNotBlank()) {
            thisLogger().warn("Bicep language server output stderr: ${testOutput.stderr}")
            return false
        }

        return true
    }

    private fun isInfrastructurePresent(): Boolean {
        return LsInfrastructure.allKnown().all(LsInfrastructure::isPresent)
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