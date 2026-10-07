/*
 * Copyright 2018-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the MIT license.
 */

@file:Suppress("UnstableApiUsage")

package com.microsoft.azure.toolkit.intellij.identity

import com.intellij.icons.AllIcons
import com.intellij.openapi.observable.properties.AtomicBooleanProperty
import com.intellij.openapi.options.BoundConfigurable
import com.intellij.openapi.project.Project
import com.intellij.ui.components.JBList
import com.intellij.ui.dsl.builder.Align
import com.intellij.ui.dsl.builder.panel
import com.intellij.ui.layout.ComponentPredicate
import com.intellij.ui.layout.and
import com.intellij.ui.layout.not
import com.microsoft.azure.toolkit.ide.common.auth.IdeAzureAccount
import com.microsoft.azure.toolkit.intellij.AzureToolkitService
import com.microsoft.azure.toolkit.intellij.common.subscription.SubscriptionsDialog
import com.microsoft.azure.toolkit.lib.Azure
import com.microsoft.azure.toolkit.lib.auth.AuthConfiguration
import com.microsoft.azure.toolkit.lib.auth.AuthType
import com.microsoft.azure.toolkit.lib.auth.AzureAccount
import com.microsoft.azure.toolkit.lib.auth.cli.AzureCliAccount
import com.microsoft.azure.toolkit.lib.common.model.Subscription
import com.microsoft.azure.toolkit.lib.common.task.AzureTaskManager
import kotlinx.coroutines.launch

class IdentityConfigurable(private val project: Project) : BoundConfigurable("Azure Identity") {
    private val isLoggedInProperty = AtomicBooleanProperty(false)
    private val isLoggedInPredicate =
        ComponentPredicate.fromObservableProperty(isLoggedInProperty)
    private val isLoggedInWithAzureCliProperty = AtomicBooleanProperty(false)
    private val isLoggedInWithAzureCliPredicate =
        ComponentPredicate.fromObservableProperty(isLoggedInWithAzureCliProperty)
    private val isAzureCLiAuthAvailableProperty = AtomicBooleanProperty(false)
    private val isAzureCLiAuthAvailablePredicate =
        ComponentPredicate.fromObservableProperty(isAzureCLiAuthAvailableProperty)

    private val subscriptionsList = JBList(emptyList<Subscription>()).apply {
        visibleRowCount = 4
        cellRenderer = SubscriptionDetailRenderer()
        setEmptyText("No subscriptions can be accessed using your current Azure CLI credentials.")
    }

    override fun createPanel() = panel {
        row("When logged in with Azure CLI, your apps can use your developer credentials to authenticate\nand access Azure resources when debugging.") {}
        row {
            browserLink(
                "How does Azure Identity client library work?",
                "https://github.com/Azure/azure-sdk-for-net/blob/main/sdk/identity/Azure.Identity/README.md"
            )
        }
        row {
            button("Sign in with Azure CLI...") {
                val type = AuthConfiguration(AuthType.AZURE_CLI)
                val account = Azure.az(AzureAccount::class.java)
                val login = account.login(type, Azure.az().config().isAuthPersistenceEnabled)
                if (login.isLoggedIn) {
                    val manager = AzureTaskManager.getInstance()
                    manager.runLater {
                        val dialog = SubscriptionsDialog(project)
                        dialog.select { selected ->
                            Azure.az(AzureAccount::class.java).account().setSelectedSubscriptions(selected)
                            update()
                        }
                    }
                }
            }.enabledIf(isAzureCLiAuthAvailablePredicate)
        }.visibleIf(isLoggedInPredicate.not())
        row {
            button("Sign Out...") {
                val account = Azure.az(AzureAccount::class.java)
                account.logout()
                update()
            }
        }.visibleIf(isLoggedInPredicate)
        row {
            label("You are not signed in with Azure CLI. Please, execute `az login` command.").applyToComponent {
                icon = AllIcons.General.Warning
            }
        }.visibleIf(isAzureCLiAuthAvailablePredicate.not())
        row {
            label("You are currently signed in with other method than Azure CLI.").applyToComponent {
                icon = AllIcons.General.Warning
            }
        }.visibleIf(isAzureCLiAuthAvailablePredicate and isLoggedInPredicate and isLoggedInWithAzureCliPredicate.not())
        groupRowsRange("Subscriptions", indent = false) {
            row {
                label("You are currently signed in with Azure CLI. Your apps can authenticate using your developer credentials.").applyToComponent {
                    icon = AllIcons.General.InspectionsOK
                }
            }
            row {
                placeholder()
            }
            row {
                label("Resources in the following subscriptions may be accessible from your application code:")
            }
            row {
                scrollCell(subscriptionsList)
                    .align(Align.FILL)
            }.resizableRow()
        }.visibleIf(isLoggedInWithAzureCliPredicate)
    }

    init {
        AzureToolkitService.getInstance(project).scope.launch {
            update()
        }
    }

    private fun update() {
        isLoggedInProperty.set(isLoggedIn())
        isLoggedInWithAzureCliProperty.set(isLoggedInWithCli())
        isAzureCLiAuthAvailableProperty.set(AuthType.AZURE_CLI.checkAvailable())
        subscriptionsList.setListData(
            Azure.az(AzureAccount::class.java).account?.selectedSubscriptions?.toTypedArray() ?: emptyArray()
        )
    }

    private fun isLoggedIn() = IdeAzureAccount.getInstance().isLoggedIn

    private fun isLoggedInWithCli(): Boolean {
        val isLoggedIn = isLoggedIn()
        if (!isLoggedIn) return false
        val account = Azure.az(AzureAccount::class.java).account
        return account is AzureCliAccount
    }
}
