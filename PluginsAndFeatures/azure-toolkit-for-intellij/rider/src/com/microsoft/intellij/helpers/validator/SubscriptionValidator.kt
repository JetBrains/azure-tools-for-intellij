/**
 * Copyright (c) 2018-2020 JetBrains s.r.o.
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

package com.microsoft.intellij.helpers.validator

import com.microsoft.azure.management.resources.Subscription
import com.microsoft.azure.management.resources.SubscriptionState
import org.jetbrains.plugins.azure.RiderAzureBundle.message

object SubscriptionValidator: AzureResourceValidator() {

    fun validateSubscription(subscription: Subscription?): ValidationResult {
        val status = checkSubscriptionIsSet(subscription)
        if (!status.isValid) return status

        return validateSubscriptionState(subscription!!)
    }

    fun checkSubscriptionIsSet(subscription: Subscription?) =
            checkValueIsSet(subscription, message("run_config.publish.validation.subscription.not_defined"))

    private fun validateSubscriptionState(subscription: Subscription): ValidationResult {
        val status = ValidationResult()

        val subscriptionState = subscription.state()
        if (subscriptionState == SubscriptionState.DISABLED)
            return status.setInvalid(message("run_config.publish.validation.subscription.disabled", subscription.displayName()))

        if (subscriptionState == SubscriptionState.DELETED)
            return status.setInvalid(message("run_config.publish.validation.subscription.deleted", subscription.displayName()))

        return status
    }
}