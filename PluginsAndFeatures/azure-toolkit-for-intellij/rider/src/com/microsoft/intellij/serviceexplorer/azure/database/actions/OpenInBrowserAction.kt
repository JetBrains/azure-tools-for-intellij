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

package com.microsoft.intellij.serviceexplorer.azure.database.actions

import com.intellij.ide.BrowserUtil
import com.intellij.openapi.project.Project
import com.microsoft.azuretools.authmanage.AuthMethodManager
import com.microsoft.azuretools.core.mvp.model.AzureMvpModel
import com.microsoft.azuretools.ijidea.actions.AzureSignInAction
import com.microsoft.intellij.AzurePlugin
import com.microsoft.tooling.msservices.serviceexplorer.Node
import com.microsoft.tooling.msservices.serviceexplorer.NodeActionEvent
import com.microsoft.tooling.msservices.serviceexplorer.NodeActionListener

abstract class OpenInBrowserAction(private val subscriptionId: String, private val node: Node)
    : NodeActionListener() {

    override fun actionPerformed(event: NodeActionEvent?) {
        try {
            val project = node.project as? Project ?: return
            if (!AzureSignInAction.doSignIn(AuthMethodManager.getInstance(), project)) return

            val url = AzureMvpModel.getInstance().getResourceUri(subscriptionId, node.id)
                    ?: throw RuntimeException("Unable to get URL for resource: '${node.id}'")

            BrowserUtil.browse(url)
        } catch (e: Throwable) {
            val message = "Error opening resource with id '${node.id}' in browser: $e"
            AzurePlugin.log(message)
            throw RuntimeException(message)
        }
    }

    override fun getIconPath(): String = "OpenInBrowser.svg"
}
