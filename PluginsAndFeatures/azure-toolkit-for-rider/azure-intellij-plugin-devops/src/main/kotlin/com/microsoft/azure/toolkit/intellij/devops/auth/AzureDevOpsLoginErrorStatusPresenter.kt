package com.microsoft.azure.toolkit.intellij.devops.auth

import com.intellij.collaboration.auth.ui.login.LoginException
import com.intellij.collaboration.messages.CollaborationToolsBundle
import com.intellij.collaboration.ui.ExceptionUtil
import com.intellij.collaboration.ui.codereview.list.error.ErrorStatusPresenter
import com.intellij.collaboration.ui.util.swingAction
import com.intellij.openapi.util.NlsSafe
import com.intellij.openapi.util.text.HtmlBuilder
import com.intellij.openapi.util.text.HtmlChunk
import com.microsoft.azure.toolkit.intellij.devops.auth.ui.AzureDevOpsTokenLoginPanelModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import java.net.ConnectException
import javax.swing.Action

internal class AzureDevOpsLoginErrorStatusPresenter(
    private val cs: CoroutineScope,
    private val model: AzureDevOpsTokenLoginPanelModel,
) : ErrorStatusPresenter.HTML<Throwable> {

    override fun getErrorTitle(error: Throwable): String = ""

    override fun getHTMLBody(error: Throwable): @NlsSafe String {
        val builder = HtmlBuilder()
        when (error) {
            is ConnectException -> builder.append(CollaborationToolsBundle.message("clone.dialog.login.error.server"))
            is LoginException.InvalidTokenOrUnsupportedServerVersion -> builder.append("Invalid token")
            is LoginException.AccountAlreadyExists -> builder.append(
                CollaborationToolsBundle.message(
                    "login.dialog.error.account.already.exists",
                    error.username
                )
            )

            is LoginException.AccountUsernameMismatch -> builder.append(
                CollaborationToolsBundle.message(
                    "login.dialog.error.account.username.mismatch",
                    error.requiredUsername,
                    error.username
                )
            )

            else -> builder.append(ExceptionUtil.getPresentableMessage(error))
        }

        return builder.wrapWithHtmlBody().toString()
    }

    override fun getErrorAction(error: Throwable): Action? = when (error) {
        is LoginException.UnsupportedServerVersion,
        is LoginException.InvalidTokenOrUnsupportedServerVersion -> swingAction(CollaborationToolsBundle.message("login.via.git")) {
            cs.launch {
                model.tryGitAuthorization()
            }
        }

        else -> null
    }
}
