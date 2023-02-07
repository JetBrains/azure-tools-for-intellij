///**
// * Copyright (c) 2018-2022 JetBrains s.r.o.
// *
// * All rights reserved.
// *
// * MIT License
// *
// * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
// * documentation files (the "Software"), to deal in the Software without restriction, including without limitation
// * the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and
// * to permit persons to whom the Software is furnished to do so, subject to the following conditions:
// *
// * The above copyright notice and this permission notice shall be included in all copies or substantial portions of
// * the Software.
// *
// * THE SOFTWARE IS PROVIDED *AS IS*, WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO
// * THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
// * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT,
// * TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
// * SOFTWARE.
// */
//package org.jetbrains.plugins.azure.cloudshell.terminal
//
//import com.intellij.ide.ui.IdeUiService
//import com.intellij.openapi.project.Project
//import com.intellij.util.net.ssl.CertificateManager
//import org.jetbrains.plugins.azure.cloudshell.rest.CloudConsoleService
//import org.jetbrains.plugins.azure.cloudshell.rest.CloudConsoleTerminalWebSocket
//import java.net.URI
//
//@Suppress("UnstableApiUsage")
//class AzureCloudTerminalFactory {
//    companion object {
//        fun createTerminalRunner(project: Project,
//                                 cloudConsoleService: CloudConsoleService,
//                                 cloudConsoleBaseUrl: String,
//                                 socketUri: URI): AzureCloudTerminalRunner {
//            // Connect terminal web socket
//            val terminalSocketClient = CloudConsoleTerminalWebSocket(socketUri)
//
//            IdeUiService.getInstance()?.sslSocketFactory?.let {
//                // Inject IDEA SSL socket factory
//                terminalSocketClient.setSocketFactory(it)
//            }
//
//            terminalSocketClient.connectBlocking()
//
//            // Create runner
//            return AzureCloudTerminalRunner(
//                    project, cloudConsoleService, cloudConsoleBaseUrl, socketUri, AzureCloudTerminalProcess(terminalSocketClient))
//        }
//    }
//}