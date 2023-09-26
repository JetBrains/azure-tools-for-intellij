/**
 * Copyright (c) 2020 JetBrains s.r.o.
 * <p/>
 * All rights reserved.
 * <p/>
 * MIT License
 * <p/>
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation files (the "Software"), to deal in the Software without restriction, including without limitation
 * the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and
 * to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 * <p/>
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of
 * the Software.
 * <p/>
 * THE SOFTWARE IS PROVIDED *AS IS*, WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO
 * THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT,
 * TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package org.jetbrains.plugins.azure.identity.ad.ui

import com.intellij.openapi.Disposable
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.ui.ValidationInfo
import com.intellij.openapi.util.Disposer
import com.intellij.ui.components.JBTextField
import com.intellij.ui.dsl.builder.AlignX
import com.intellij.ui.dsl.builder.bindSelected
import com.intellij.ui.dsl.builder.bindText
import com.intellij.ui.dsl.builder.panel
import org.jetbrains.plugins.azure.RiderAzureBundle
import org.jetbrains.plugins.azure.isValidGuid
import org.jetbrains.plugins.azure.isValidUrl

class RegisterApplicationInAzureAdDialog(project: Project, val model: RegistrationModel)
    : DialogWrapper(project), Disposable {

    class RegistrationModel(
            val originalDisplayName: String,
            val originalClientId: String,
            val originalDomain: String,
            val originalCallbackUrl: String,
            val originalIsMultiTenant: Boolean,
            var allowOverwrite: Boolean
    ) {
        var updatedDisplayName: String = originalDisplayName
        var updatedDomain: String = originalDomain
        var updatedClientId: String = originalClientId
        var updatedCallbackUrl: String = originalCallbackUrl
        var updatedIsMultiTenant: Boolean = originalIsMultiTenant
        var hasChanges: Boolean = false
    }

    private val panel = panel {

        row {
            label(RiderAzureBundle.message("dialog.identity.ad.register_app.description"))
            browserLink(RiderAzureBundle.message("dialog.identity.ad.register_app.description.link"), "https://go.microsoft.com/fwlink/?LinkId=299424")
        }

        row(RiderAzureBundle.message("dialog.identity.ad.register_app.display_name")) {
            textField()
                    .align(AlignX.FILL)
                    .bindText(
                            { model.originalDisplayName },
                            {
                                model.updatedDisplayName = it.trim()
                                model.hasChanges = true
                            }
                    )
                    .validationOnInput { validateNotEmpty(it) }
        }

        row(RiderAzureBundle.message("dialog.identity.ad.register_app.callback_url")) {
            textField()
                    .align(AlignX.FILL)
                    .bindText(
                            { model.originalCallbackUrl },
                            {
                                model.updatedCallbackUrl = it.trim()
                                model.hasChanges = true
                            }
                    )
                    .validationOnInput { validateNotEmpty(it) ?: validateUrl(it) }
        }

        row(RiderAzureBundle.message("dialog.identity.ad.register_app.domain")) {
            textField()
                    .align(AlignX.FILL)
                    .bindText(
                            { model.originalDomain },
                            {
                                model.updatedDomain = it.trim()
                                model.hasChanges = true
                            }
                    )
                    .validationOnInput { validateNotEmpty(it) }
        }

        row {
            checkBox(RiderAzureBundle.message("dialog.identity.ad.register_app.is_multi_tenant"))
                    .bindSelected(
                            { model.originalIsMultiTenant },
                            {
                                model.updatedIsMultiTenant = it
                                model.hasChanges = true
                            }
                    )
        }

        collapsibleGroup(RiderAzureBundle.message("dialog.identity.ad.register_app.more_options")) {
            row(RiderAzureBundle.message("dialog.identity.ad.register_app.client_id")) {

                textField()
                        .align(AlignX.FILL)
                        .bindText(
                                { model.originalClientId },
                                {
                                    model.updatedClientId = it.trim()
                                    model.hasChanges = true
                                }
                        )
                        .validationOnInput {
                            if (!it.text?.trim().isNullOrEmpty()) {
                                validateUuid(it)
                            } else
                                null
                        }
            }

            row {
                checkBox(RiderAzureBundle.message("dialog.identity.ad.register_app.allow_overwrite"))
                        .bindSelected(
                                { model.allowOverwrite },
                                {
                                    model.allowOverwrite = it
                                    model.hasChanges = true
                                }
                        )
            }
        }
    }

    private fun validateNotEmpty(textField: JBTextField) =
            if (textField.text?.trim().isNullOrEmpty()) {
                ValidationInfo(RiderAzureBundle.message("dialog.identity.ad.register_app.validation.can_not_be_empty"), textField)
            } else {
                null
            }

    private fun validateUrl(textField: JBTextField) =
            if (!textField.text?.trim().isValidUrl()) {
                ValidationInfo(RiderAzureBundle.message("dialog.identity.ad.register_app.validation.must_be_valid_url"), textField)
            } else {
                null
            }

    private fun validateUuid(textField: JBTextField) =
            if (!textField.text?.trim().isValidGuid()) {
                ValidationInfo(RiderAzureBundle.message("dialog.identity.ad.register_app.validation.must_be_valid_identifier"), textField)
            } else {
                null
            }

    init {
        Disposer.register(project, this)
        title = RiderAzureBundle.message("dialog.identity.ad.register_app.title")
        setOKButtonText(RiderAzureBundle.message("dialog.identity.ad.register_app.register"))
        setSize(500, -1)
        super.init()
    }

    override fun createCenterPanel() = panel.apply {
        reset()
    }

    override fun doOKAction() {
        panel.apply()
        super.doOKAction()
    }

    override fun dispose() {
        super.dispose()
    }
}