//package com.microsoft.azure.toolkit.intellij.appservice.task;
//
//import com.microsoft.azure.toolkit.ide.guidance.ComponentContext;
//import com.microsoft.azure.toolkit.ide.guidance.Task;
//import com.microsoft.azure.toolkit.lib.Azure;
//import com.microsoft.azure.toolkit.lib.appservice.webapp.AzureWebApp;
//import com.microsoft.azure.toolkit.lib.appservice.webapp.WebApp;
//import com.microsoft.azure.toolkit.lib.common.action.AzureActionManager;
//import com.microsoft.azure.toolkit.lib.common.operation.AzureOperation;
//
//import javax.annotation.Nonnull;
//import java.util.Objects;
//
//import static com.microsoft.azure.toolkit.ide.appservice.AppServiceActionsContributor.OPEN_IN_BROWSER;
//
//
//public class OpenInBrowserTask implements Task {
//
//    private final ComponentContext context;
//
//    public OpenInBrowserTask(@Nonnull final ComponentContext context) {
//        this.context = context;
//    }
//
//    @Override
//    @AzureOperation(name = "user/guidance.open_in_browser")
//    public void execute() throws Exception {
//        final String webAppId = Objects.requireNonNull((String) context.getParameter("webappId"), "Failed to get the web app id created in tasks");
//        final WebApp webApp = Objects.requireNonNull(Azure.az(AzureWebApp.class).webApp(webAppId), String.format("failed to find web app with id (%s) in Azure", webAppId));
//        AzureActionManager.getInstance().getAction(OPEN_IN_BROWSER).handle(webApp);
//    }
//
//    @Nonnull
//    @Override
//    public String getName() {
//        return "task.common.open_browser";
//    }
//}
