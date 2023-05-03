package com.microsoft.intellij.helpers

import com.google.common.util.concurrent.ListenableFuture
import com.intellij.openapi.application.ApplicationManager
import com.microsoft.azure.toolkit.lib.common.task.AzureTask
import com.microsoft.azure.toolkit.lib.common.task.AzureTaskManager
import com.microsoft.tooling.msservices.helpers.IDEHelper

class IDEHelperImpl : IDEHelper {
    override fun getProjectSettingsPath(): String {
        TODO("Not yet implemented")
    }

    override fun closeFile(p0: Any?, p1: Any?) {
        TODO("Not yet implemented")
    }

    override fun invokeLater(runnable: Runnable) {
        AzureTaskManager.getInstance().runLater(runnable, AzureTask.Modality.ANY)
    }

    override fun invokeAndWait(runnable: Runnable) {
        AzureTaskManager.getInstance().runAndWait(runnable, AzureTask.Modality.ANY)
    }

    override fun executeOnPooledThread(runnable: Runnable) {
        ApplicationManager.getApplication().executeOnPooledThread(runnable)
    }

    override fun getArtifacts(p0: IDEHelper.ProjectDescriptor?): MutableList<IDEHelper.ArtifactDescriptor> {
        TODO("Not yet implemented")
    }

    override fun buildArtifact(
        p0: IDEHelper.ProjectDescriptor?,
        p1: IDEHelper.ArtifactDescriptor?
    ): ListenableFuture<String> {
        TODO("Not yet implemented")
    }

    override fun getCurrentProject(): Any {
        TODO("Not yet implemented")
    }

    override fun setApplicationProperty(p0: String?, p1: String?) {
        TODO("Not yet implemented")
    }

    override fun unsetApplicationProperty(p0: String?) {
        TODO("Not yet implemented")
    }

    override fun getApplicationProperty(p0: String?): String {
        TODO("Not yet implemented")
    }

    override fun setApplicationProperties(p0: String?, p1: Array<out String>?) {
        TODO("Not yet implemented")
    }

    override fun unsetApplicatonProperties(p0: String?) {
        TODO("Not yet implemented")
    }

    override fun getApplicationProperties(p0: String?): Array<String> {
        TODO("Not yet implemented")
    }

    override fun isApplicationPropertySet(p0: String?): Boolean {
        TODO("Not yet implemented")
    }

    override fun openLinkInBrowser(p0: String?) {
        TODO("Not yet implemented")
    }
}