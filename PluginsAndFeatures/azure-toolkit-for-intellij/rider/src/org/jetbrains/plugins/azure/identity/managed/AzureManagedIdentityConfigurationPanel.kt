/**
 * Copyright (c) 2020-2023 JetBrains s.r.o.
 *
 * All rights reserved.
 *
 * MIT License
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation files (the "Software"), to deal in the Software without restriction, including without limitation
 * the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and
 * to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of
 * the Software.
 *
 * THE SOFTWARE IS PROVIDED *AS IS*, WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO
 * THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT,
 * TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package org.jetbrains.plugins.azure.identity.managed

import com.intellij.icons.AllIcons
import com.intellij.ide.BrowserUtil
import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogPanel
import com.intellij.ui.components.JBList
import com.intellij.ui.dsl.builder.Row
import com.intellij.ui.dsl.builder.RowsRange
import com.intellij.ui.dsl.builder.panel
import com.intellij.util.application
import com.intellij.util.ui.UIUtil
import com.microsoft.azuretools.authmanage.AuthMethod
import com.microsoft.azuretools.authmanage.AuthMethodManager
import com.microsoft.azuretools.authmanage.models.SubscriptionDetail
import com.microsoft.intellij.actions.AzureSignInAction
import com.microsoft.intellij.configuration.AzureRiderAbstractConfigurable
import net.miginfocom.swing.MigLayout
import org.jetbrains.plugins.azure.RiderAzureBundle
import org.jetbrains.plugins.azure.RiderAzureBundle.message
import java.io.IOException

@Suppress("UNUSED_LAMBDA_EXPRESSION")
class AzureManagedIdentityConfigurationPanel(parentDisposable: Disposable, private val project: Project)
    : AzureRiderAbstractConfigurable(message("settings.managedidentity.name"), parentDisposable) {

    private val logger = Logger.getInstance(AzureManagedIdentityConfigurationPanel::class.java)

    private fun createPanel(): DialogPanel {
        lateinit var dialogPanel: DialogPanel
        dialogPanel = panel {
            row(message("settings.managedidentity.description")) {}
            row {
                link(RiderAzureBundle.message("settings.managedidentity.info.title")) {
                    BrowserUtil.open(RiderAzureBundle.message("settings.managedidentity.info.url"))
                }
            }

            // When not signed in, show sign in button
            val signInRow: Row = row {
                button(RiderAzureBundle.message("settings.managedidentity.sign_in")) {
                    AzureSignInAction.doSignIn(AuthMethodManager.getInstance(), project)
                            .subscribe {
                                application.invokeLater {
                                    dialogPanel.reset()
                                }
                            }
                }
            }

            // When signed in, show sign out button
            val signOutRow: Row = row {
                button(RiderAzureBundle.message("settings.managedidentity.sign_out")) {
                    try {
                        AuthMethodManager.getInstance().signOut()
                        dialogPanel.reset()
                    } catch (e: Exception) {
                        logger.error("Error while signing out", e)
                    }
                }
            }

            // Shown when not signed in with Azure Cli
            val rowNotSignedInWithAzureCli: Row = row {
                label(RiderAzureBundle.message("settings.managedidentity.not_signed_in_with_cli")).applyToComponent {
                    icon = AllIcons.General.Warning
                }
            }

            // Shown when signed in with Azure Cli
            lateinit var subscriptionsList: JBList<SubscriptionDetail>
            val rowSignedInWithAzureCli: RowsRange = groupRowsRange {
                row {
                    label(RiderAzureBundle.message("settings.managedidentity.signed_in_with_cli")).applyToComponent {
                        icon = AllIcons.General.InspectionsOK
                    }
                }

                row {
                    placeholder()
                }

                row {
                    label(RiderAzureBundle.message("settings.managedidentity.signed_in_with_cli.accessible_subscriptions"))
                }

                row {
                    subscriptionsList = JBList(emptyList<SubscriptionDetail>()).apply {
                        cellRenderer = SubscriptionDetailRenderer()
                        setEmptyText(RiderAzureBundle.message("settings.managedidentity.signed_in_with_cli.accessible_subscriptions"))
                    }
                    scrollCell(subscriptionsList)
                }
            }

            row {
                placeholder()
            }

            onReset {
                signInRow.visible(!isSignedIn())
                signOutRow.visible(isSignedIn())
                rowNotSignedInWithAzureCli.visible(!isSignedInWithAzureCli())
                rowSignedInWithAzureCli.visible(isSignedInWithAzureCli())

                ApplicationManager.getApplication().executeOnPooledThread {
                    val subscriptionDetails = try {
                        AuthMethodManager.getInstance()
                                ?.azureManager?.subscriptionManager?.subscriptionDetails
                    } catch (e: IOException) {
                        logger.error("Error while retrieving subscription details", e)
                        null
                    }

                    UIUtil.invokeAndWaitIfNeeded(Runnable {
                        if (isSignedInWithAzureCli()) {
                            subscriptionsList.setListData(subscriptionDetails?.toTypedArray()
                                    ?: emptyArray<SubscriptionDetail>())

                            dialogPanel.revalidate()
                            dialogPanel.repaint()
                        }
                    })
                }
            }
        }

        dialogPanel.layout = MigLayout("novisualpadding, ins 0, fillx, wrap 1, hidemode 3")
        return dialogPanel
    }

    private fun isSignedIn() = try {
        AuthMethodManager.getInstance().isSignedIn
    } catch (e: Exception) {
        false
    }

    private fun isSignedInWithAzureCli() = try {
        val authMethodManager = AuthMethodManager.getInstance()

        authMethodManager.isSignedIn
                && authMethodManager.authMethod == AuthMethod.AZ
    } catch (e: Exception) {
        false
    }

    override val panel = createPanel().apply {
        reset()
    }

    override fun isProjectLevel() = true
}