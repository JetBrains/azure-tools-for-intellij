package com.microsoft.azure.toolkit.intellij.devops.auth

import com.intellij.collaboration.auth.ui.login.LoginModel
import com.intellij.collaboration.auth.ui.login.TokenLoginDialog
import com.intellij.collaboration.auth.ui.login.TokenLoginInputPanelFactory
import com.intellij.collaboration.messages.CollaborationToolsBundle
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.util.NlsContexts
import com.intellij.util.asSafely
import com.intellij.util.concurrency.annotations.RequiresEdt
import com.microsoft.azure.toolkit.intellij.devops.api.AzureDevOpsServerPath
import com.microsoft.azure.toolkit.intellij.devops.auth.accounts.AzureDevOpsAccount
import com.microsoft.azure.toolkit.intellij.devops.auth.accounts.AzureDevOpsProjectDefaultAccountHolder
import com.microsoft.azure.toolkit.intellij.devops.auth.ui.AzureDevOpsChooseAccountDialog
import com.microsoft.azure.toolkit.intellij.devops.auth.ui.AzureDevOpsTokenLoginPanelModel
import com.microsoft.azure.toolkit.intellij.devops.util.AzureDevOpsPluginProjectScopeProvider
import org.jetbrains.annotations.Nls
import java.awt.Component
import javax.swing.JComponent

object AzureDevOpsLoginUtil {

    @RequiresEdt
    internal fun logInViaToken(
        project: Project, parentComponent: JComponent?,
        serverPath: AzureDevOpsServerPath = AzureDevOpsServerPath.DEFAULT_PATH,
        uniqueAccountPredicate: (AzureDevOpsServerPath, String) -> Boolean
    ): LoginResult {

        val model = AzureDevOpsTokenLoginPanelModel(uniqueAccountPredicate).apply {
            serverUri = serverPath.uri
        }

        val dialogTitle = "Add Azure DevOps Organization"
        val exitCode = showLoginDialog(project, parentComponent, model, dialogTitle, false)
        return when (exitCode) {
            DialogWrapper.OK_EXIT_CODE -> {
                val loginResult = model.loginState.value.asSafely<LoginModel.LoginState.Connected>() ?: return LoginResult.Failure
                return LoginResult.Success(AzureDevOpsAccount(name = loginResult.username, server = model.getServerPath()), model.token)
            }
            DialogWrapper.NEXT_USER_EXIT_CODE -> LoginResult.OtherMethod
            else -> LoginResult.Failure
        }
    }

    @RequiresEdt
    internal fun updateToken(
        project: Project, parentComponent: JComponent?,
        account: AzureDevOpsAccount,
        uniqueAccountPredicate: (AzureDevOpsServerPath, String) -> Boolean
    ): LoginResult {
        val predicateWithoutCurrent: (AzureDevOpsServerPath, String) -> Boolean = { serverPath, username ->
            if (serverPath == account.server && username == account.name) true
            else uniqueAccountPredicate(serverPath, username)
        }

        val model = AzureDevOpsTokenLoginPanelModel(predicateWithoutCurrent).apply {
            serverUri = account.server.uri
        }
        val title = "Update Azure DevOps Organization"
        val exitState = showLoginDialog(project, parentComponent, model, title, true)
        val loginState = model.loginState.value
        if (exitState == DialogWrapper.OK_EXIT_CODE && loginState is LoginModel.LoginState.Connected) {
            return LoginResult.Success(
                AzureDevOpsAccount(id = account.id, name = loginState.username, server = model.getServerPath()),
                model.token
            )
        }

        return LoginResult.Failure
    }

    @RequiresEdt
    private fun showLoginDialog(
        project: Project,
        parentComponent: JComponent?,
        model: AzureDevOpsTokenLoginPanelModel,
        title: @NlsContexts.DialogTitle String,
        serverFieldDisabled: Boolean
    ): Int {
        val scopeProvider = project.service<AzureDevOpsPluginProjectScopeProvider>()
        val dialog = scopeProvider.constructDialog("Azure DevOps token login dialog") {
            TokenLoginDialog(project, this, parentComponent, model, title, model.tryGitAuthorizationSignal) {
                val cs = this
                TokenLoginInputPanelFactory(model).createIn(
                    cs,
                    serverFieldDisabled,
                    tokenNote = CollaborationToolsBundle.message("clone.dialog.insufficient.scopes", ""),
                    errorPresenter = AzureDevOpsLoginErrorStatusPresenter(cs, model)
                )
            }
        }
        dialog.showAndGet()

        return dialog.exitCode
    }

    @RequiresEdt
    internal fun chooseAccount(project: Project,
                               parentComponent: Component?,
                               description: @Nls String?,
                               accounts: Collection<AzureDevOpsAccount>): AzureDevOpsAccount? {
        val dialog = AzureDevOpsChooseAccountDialog(project, parentComponent, accounts, false, true, description = description)
        return if (dialog.showAndGet()) {
            val account = dialog.account
            if (dialog.setDefault) {
                project.service<AzureDevOpsProjectDefaultAccountHolder>().account = account
            }
            account
        }
        else {
            null
        }
    }

    fun isAccountUnique(accounts: Collection<AzureDevOpsAccount>, server: AzureDevOpsServerPath, accountName: String): Boolean =
        accounts.none { it.server.toHttpsURI() == server.toHttpsURI() && it.name == accountName }

}

sealed interface LoginResult {
    data class Success(val account: AzureDevOpsAccount, val token: String) : LoginResult
    data object Failure : LoginResult
    data object OtherMethod : LoginResult
}
