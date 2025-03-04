/*
 * Copyright 2018-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the MIT license.
 */

@file:Suppress("InvalidBundleOrProperty")

package com.microsoft.azure.toolkit.intellij.appservice

import com.intellij.openapi.actionSystem.AnActionEvent
import com.microsoft.azure.toolkit.ide.appservice.AppServiceActionsContributor
import com.microsoft.azure.toolkit.ide.appservice.file.AppServiceFileActionsContributor
import com.microsoft.azure.toolkit.ide.appservice.function.FunctionAppActionsContributor.FUNCTION_APP_ACTIONS
import com.microsoft.azure.toolkit.ide.appservice.webapp.WebAppActionsContributor.WEBAPP_ACTIONS
import com.microsoft.azure.toolkit.ide.common.IActionsContributor
import com.microsoft.azure.toolkit.ide.common.action.ResourceCommonActionsContributor
import com.microsoft.azure.toolkit.ide.common.icon.AzureIcons
import com.microsoft.azure.toolkit.ide.containerregistry.ContainerRegistryActionsContributor
import com.microsoft.azure.toolkit.intellij.appservice.actions.AppServiceFileAction
import com.microsoft.azure.toolkit.intellij.appservice.actions.AppServiceRemoteDebuggingHandler
import com.microsoft.azure.toolkit.intellij.appservice.actions.OpenAppServicePropertyViewAction
import com.microsoft.azure.toolkit.intellij.debugger.canBeDebugged
import com.microsoft.azure.toolkit.intellij.legacy.function.actions.CreateFunctionAppAction
import com.microsoft.azure.toolkit.intellij.legacy.function.actions.DeployFunctionAppAction
import com.microsoft.azure.toolkit.intellij.legacy.webapp.action.CreateWebAppAction
import com.microsoft.azure.toolkit.intellij.legacy.webapp.action.DeployWebAppAction
import com.microsoft.azure.toolkit.lib.appservice.AppServiceAppBase
import com.microsoft.azure.toolkit.lib.appservice.function.AzureFunctions
import com.microsoft.azure.toolkit.lib.appservice.function.FunctionApp
import com.microsoft.azure.toolkit.lib.appservice.function.FunctionAppDeploymentSlot
import com.microsoft.azure.toolkit.lib.appservice.model.AppServiceFile
import com.microsoft.azure.toolkit.lib.appservice.webapp.AzureWebApp
import com.microsoft.azure.toolkit.lib.appservice.webapp.WebApp
import com.microsoft.azure.toolkit.lib.appservice.webapp.WebAppDeploymentSlot
import com.microsoft.azure.toolkit.lib.common.action.Action
import com.microsoft.azure.toolkit.lib.common.action.AzureActionManager
import com.microsoft.azure.toolkit.lib.common.task.AzureTaskManager
import kotlin.math.max

class AppServiceRiderActionsContributor : IActionsContributor {

    private val initializeOrder =
        max(AppServiceActionsContributor.INITIALIZE_ORDER, ContainerRegistryActionsContributor.INITIALIZE_ORDER) + 1

    val REMOTE_DEBUGGING: Action.Id<AppServiceAppBase<*, *, *>> = Action.Id.of("user/appservice.start_remote_debugging.app")

    override fun getOrder() = initializeOrder

    override fun registerGroups(am: AzureActionManager) {
        am.getGroup(FUNCTION_APP_ACTIONS).prependActions(REMOTE_DEBUGGING, "---")
        am.getGroup(WEBAPP_ACTIONS).prependActions(REMOTE_DEBUGGING, "---")
    }

    override fun registerActions(am: AzureActionManager) {
        val tm = AzureTaskManager.getInstance()

        Action(AppServiceFileActionsContributor.APP_SERVICE_FILE_VIEW)
            .withLabel("Open File")
            .withIdParam { file: AppServiceFile -> file.name }
            .visibleWhen { file: Any? -> file is AppServiceFile }
            .withSource { file: AppServiceFile -> file.app }
            .withHandler { file: AppServiceFile, e: Any ->
                tm.runLater {
                    AppServiceFileAction().openAppServiceFile(file, (e as AnActionEvent).project)
                }
            }
            .withShortcut(am.ideDefaultShortcuts.edit())
            .register(am)

        Action(AppServiceFileActionsContributor.APP_SERVICE_FILE_DOWNLOAD)
            .withLabel("Download")
            .withIdParam { file: AppServiceFile -> file.name }
            .visibleWhen { file: Any? -> file is AppServiceFile }
            .withSource { file: AppServiceFile -> file.app }
            .withHandler { file: AppServiceFile, e: Any ->
                tm.runLater {
                    AppServiceFileAction().saveAppServiceFile(file, (e as AnActionEvent).project, null)
                }
            }
            .register(am)

        Action(REMOTE_DEBUGGING)
            .withLabel("Attach Debugger")
            .withIcon(AzureIcons.Action.ATTACH_DEBUGGER.iconPath)
            .withIdParam { appService: AppServiceAppBase<*, *, *> -> appService.name }
            .visibleWhen { s: Any? -> s is AppServiceAppBase<*, *, *> }
            .enableWhen { appService: AppServiceAppBase<*, *, *> -> appService.canBeDebugged() }
            .register(am)
    }

    override fun registerHandlers(am: AzureActionManager) {
        am.registerHandler(
            ResourceCommonActionsContributor.CREATE,
            { r, _ -> r is AzureWebApp },
            { _, e: AnActionEvent -> e.project?.let { CreateWebAppAction.openDialog(it) } })
        am.registerHandler(
            ResourceCommonActionsContributor.CREATE,
            { r, _ -> r is AzureFunctions },
            { _, e: AnActionEvent -> e.project?.let { CreateFunctionAppAction.openDialog(it) } })
        am.registerHandler(
            ResourceCommonActionsContributor.DEPLOY,
            { r, _ -> r is WebApp },
            { c, e: AnActionEvent -> DeployWebAppAction.deploy(c as? WebApp, e.project) })
        am.registerHandler(
            ResourceCommonActionsContributor.DEPLOY,
            { r, _ -> r is FunctionApp },
            { c, e: AnActionEvent -> DeployFunctionAppAction.deploy(c as? FunctionApp, e.project) })
        am.registerHandler(
            ResourceCommonActionsContributor.SHOW_PROPERTIES,
            { r, _ -> r is WebApp },
            { c, e: AnActionEvent ->
                e.project?.let { OpenAppServicePropertyViewAction.openWebAppPropertyView(c as WebApp, it) }
            })
        am.registerHandler(
            ResourceCommonActionsContributor.SHOW_PROPERTIES,
            { r, _ -> r is WebAppDeploymentSlot },
            { c, e: AnActionEvent ->
                e.project?.let { OpenAppServicePropertyViewAction.openDeploymentSlotPropertyView(c as WebAppDeploymentSlot, it) }
            })
        am.registerHandler(
            ResourceCommonActionsContributor.SHOW_PROPERTIES,
            { r, _ -> r is FunctionApp },
            { c, e: AnActionEvent ->
               e.project?.let { OpenAppServicePropertyViewAction.openFunctionAppPropertyView(c as FunctionApp, it) }
            })
        am.registerHandler(
            ResourceCommonActionsContributor.SHOW_PROPERTIES,
            { r, _ -> r is FunctionAppDeploymentSlot },
            { c, e: AnActionEvent ->
                e.project?.let { OpenAppServicePropertyViewAction.openFunctionAppDeploymentSlotPropertyView(c as FunctionAppDeploymentSlot, it) }
            })

        am.registerHandler(
            REMOTE_DEBUGGING,
            { r, _ -> r is  AppServiceAppBase<*, *, *> },
            AppServiceRemoteDebuggingHandler()
        )
    }
}