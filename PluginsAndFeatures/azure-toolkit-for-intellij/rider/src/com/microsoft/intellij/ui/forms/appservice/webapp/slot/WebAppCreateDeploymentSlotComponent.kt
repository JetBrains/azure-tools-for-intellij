/**
 * Copyright (c) 2020 JetBrains s.r.o.
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

package com.microsoft.intellij.ui.forms.appservice.webapp.slot

import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.ui.ValidationInfo
import com.jetbrains.rd.util.lifetime.Lifetime
import com.jetbrains.rd.util.reactive.Signal
import com.microsoft.azure.management.appservice.WebApp
import com.microsoft.intellij.helpers.validator.WebAppValidator
import com.microsoft.intellij.runner.webapp.AzureDotNetWebAppMvpModel
import com.microsoft.intellij.ui.extension.getSelectedValue
import com.microsoft.intellij.ui.extension.initValidationWithResult
import com.microsoft.intellij.ui.forms.appservice.slot.CreateDeploymentSlotComponent

class WebAppCreateDeploymentSlotComponent(private val app: WebApp,
                                          private val lifetime: Lifetime,
                                          private val isLoadFinishedSignal: Signal<Boolean>) :
        CreateDeploymentSlotComponent(app, isLoadFinishedSignal) {

    companion object {
        private val logger = Logger.getInstance(WebAppCreateDeploymentSlotComponent::class.java)
    }

    init {
        initComponentValidation()
    }

    override fun loadSlotNamesAction(): List<String> =
            AzureDotNetWebAppMvpModel.listDeploymentSlots(app, true).map { it.name() }

    override fun validateComponent(): List<ValidationInfo> =
            listOfNotNull(
                    WebAppValidator.validateDeploymentSlotName(slotName)
                            .merge(WebAppValidator.checkDeploymentSlotNameExists(app, slotName))
                            .toValidationInfo(pnlName.txtNameValue),
            ) +
            listOfNotNull(
                    if (isCloneSettings)
                        WebAppValidator.checkDeploymentSlotNameIsSet(cbExistingSettings.getSelectedValue() ?: "")
                                .toValidationInfo(cbExistingSettings)
                    else
                        null
            )

    override fun initComponentValidation() {
        pnlName.txtNameValue.initValidationWithResult(
                lifetime = lifetime,
                textChangeValidationAction = {
                    WebAppValidator.checkDeploymentSlotNameMaxLength(slotName)
                            .merge(WebAppValidator.checkDeploymentSlotNameInvalidCharacters(slotName))
                },
                focusLostValidationAction = { WebAppValidator.checkDeploymentSlotNameMinLength(slotName) })
    }
}
