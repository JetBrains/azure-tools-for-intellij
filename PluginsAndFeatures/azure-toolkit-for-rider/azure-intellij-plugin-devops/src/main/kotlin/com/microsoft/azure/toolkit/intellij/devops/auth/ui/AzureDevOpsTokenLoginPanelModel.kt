package com.microsoft.azure.toolkit.intellij.devops.auth.ui

import com.intellij.collaboration.auth.ui.login.LoginException
import com.intellij.collaboration.auth.ui.login.LoginPanelModelBase
import com.intellij.collaboration.auth.ui.login.LoginTokenGenerator
import com.intellij.collaboration.util.URIUtil
import com.intellij.ide.BrowserUtil
import com.intellij.openapi.components.service
import com.microsoft.azure.toolkit.intellij.devops.api.AzureDevOpsApiManager
import com.microsoft.azure.toolkit.intellij.devops.api.AzureDevOpsServerPath
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.withContext

class AzureDevOpsTokenLoginPanelModel(
    var uniqueAccountChecker: (AzureDevOpsServerPath, String) -> Boolean
) : LoginPanelModelBase(), LoginTokenGenerator {

    private val _tryGitAuthorizationSignal: MutableSharedFlow<Unit> = MutableSharedFlow(replay = 1)
    val tryGitAuthorizationSignal: Flow<Unit> = _tryGitAuthorizationSignal.asSharedFlow()

    override suspend fun checkToken(): String {
        val server = getServerPath(serverUri)
        val api = service<AzureDevOpsApiManager>().getClient(server, token)

        val user = withContext(Dispatchers.IO) {
            api.getCurrentUser()
        }

        val username = user.name

        if (!uniqueAccountChecker(server, username)) {
            throw LoginException.AccountAlreadyExists(username)
        }

        return user.accountName
    }

    fun getServerPath(): AzureDevOpsServerPath = getServerPath(serverUri)

    fun getServerPath(uri: String): AzureDevOpsServerPath {
        val serverPath = URIUtil.normalizeAndValidateHttpUri(uri)
        return AzureDevOpsServerPath(serverPath)
    }

    suspend fun tryGitAuthorization() = _tryGitAuthorizationSignal.emit(Unit)

    override fun canGenerateToken(serverUri: String): Boolean = URIUtil.isValidHttpUri(serverUri)

    override fun generateToken(serverUri: String) {
        val newTokenUrl = "${serverUri}/_usersSettings/tokens"
        BrowserUtil.browse(newTokenUrl)
    }
}
