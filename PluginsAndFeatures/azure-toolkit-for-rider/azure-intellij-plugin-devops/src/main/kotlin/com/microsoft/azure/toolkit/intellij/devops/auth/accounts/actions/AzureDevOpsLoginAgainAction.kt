package com.microsoft.azure.toolkit.intellij.devops.auth.accounts.actions

import com.intellij.openapi.project.Project
import com.intellij.util.asSafely
import com.microsoft.azure.toolkit.intellij.devops.auth.LoginResult
import com.microsoft.azure.toolkit.intellij.devops.auth.accounts.AzureDevOpsAccount
import com.microsoft.azure.toolkit.intellij.devops.auth.accounts.AzureDevOpsAccountManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.jetbrains.annotations.Nls
import java.awt.event.ActionEvent
import javax.swing.AbstractAction
import javax.swing.JComponent

class AzureDevOpsLoginAgainAction(
    @Nls name: String,
    private val project: Project,
    private val parentScope: CoroutineScope,
    private val account: AzureDevOpsAccount,
    private val accountManager: AzureDevOpsAccountManager,
    private val resetAction: () -> Unit = {}
) : AbstractAction(name) {
    override fun actionPerformed(event: ActionEvent) {
        val parentComponent = event.source as? JComponent ?: return
//        val loginResult = AzureDevOpsLoginUtil.updateToken(project, parentComponent, account) { _, _ -> true }
//            .asSafely<LoginResult.Success>()
//            ?: return
        parentScope.launch {
            //accountManager.updateAccount(account, loginResult.token)
            resetAction()
        }
    }
}