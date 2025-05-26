package com.microsoft.azure.toolkit.intellij.devops.ui.clone

import com.intellij.collaboration.async.nestedDisposable
import com.intellij.collaboration.auth.ui.AccountsPanelFactory.Companion.addWarningForPersistentCredentials
import com.intellij.collaboration.auth.ui.login.LoginModel
import com.intellij.collaboration.auth.ui.login.TokenLoginInputPanelFactory
import com.intellij.collaboration.messages.CollaborationToolsBundle
import com.intellij.collaboration.ui.CollaborationToolsUIUtil.focusPanel
import com.intellij.collaboration.ui.VerticalListPanel
import com.intellij.collaboration.ui.util.bindDisabledIn
import com.intellij.collaboration.ui.util.bindVisibilityIn
import com.intellij.ide.IdeBundle
import com.intellij.openapi.components.service
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.labels.LinkLabel
import com.intellij.ui.dsl.builder.AlignX
import com.intellij.util.ui.JBEmptyBorder
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.UIUtil
import com.microsoft.azure.toolkit.intellij.devops.auth.AzureDevOpsLoginErrorStatusPresenter
import com.microsoft.azure.toolkit.intellij.devops.auth.accounts.AzureDevOpsAccountManager
import com.microsoft.azure.toolkit.intellij.devops.ui.clone.model.AzureDevOpsCloneLoginViewModel
import com.microsoft.azure.toolkit.intellij.devops.ui.clone.model.AzureDevOpsCloneViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import javax.swing.JButton
import javax.swing.JComponent

internal object AzureDevOpsCloneLoginComponentFactory {

    fun create(
        cs: CoroutineScope,
        loginVm: AzureDevOpsCloneLoginViewModel,
        cloneVm: AzureDevOpsCloneViewModel
    ): JComponent {
        val loginModel = loginVm.tokenLoginModel
        val titlePanel = JBUI.Panels.simplePanel().apply {
            val title = JBLabel("Login to Azure DevOps", UIUtil.ComponentStyle.LARGE).apply {
                font = JBUI.Fonts.label().biggerOn(5.0f)
            }
            addToLeft(title)
        }

        val loginButton = JButton(CollaborationToolsBundle.message("clone.dialog.button.login.mnemonic")).apply {
            bindDisabledIn(cs, loginModel.loginState.map { it is LoginModel.LoginState.Connecting })
        }
        val backLink = LinkLabel<Unit>(IdeBundle.message("button.back"), null) { _, _ -> cloneVm.switchToRepositoriesPanel() }.apply {
            bindVisibilityIn(cs, loginVm.accounts.map { it.isNotEmpty() })
        }
        val loginInputPanel = TokenLoginInputPanelFactory(loginModel).createIn(
            cs,
            serverFieldDisabled = false,
            tokenNote = "",
            errorPresenter = AzureDevOpsLoginErrorStatusPresenter(cs, loginModel),
            footer = {
                row("") {
                    cell(loginButton)
                    cell(backLink)

                    addWarningForPersistentCredentials(
                        cs,
                        service<AzureDevOpsAccountManager>().canPersistCredentials,
                        ::panel
                    ).align(AlignX.RIGHT)
                }
            }
        ).apply {
            border = JBUI.Borders.empty(8, 0, 0, 35)
            registerValidators(cs.nestedDisposable())
        }

        loginButton.addActionListener {
            cs.launch {
                loginInputPanel.apply()
                val errors = loginInputPanel.validateAll()
                if (errors.isEmpty()) {
                    loginModel.login()
                    loginInputPanel.reset()
                }
                else {
                    val componentWithError = errors.first().component ?: return@launch
                    focusPanel(componentWithError)
                }
            }
        }

        return VerticalListPanel().apply {
            border = JBEmptyBorder(UIUtil.getRegularPanelInsets())
            add(titlePanel)
            add(loginInputPanel)
        }
    }
}
