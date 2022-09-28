package com.microsoft.azure.toolkit.intellij.appservice;

import com.microsoft.azure.toolkit.ide.guidance.ComponentContext;
import com.microsoft.azure.toolkit.ide.guidance.Context;
import com.microsoft.azure.toolkit.ide.guidance.Task;
import com.microsoft.azure.toolkit.ide.guidance.config.TaskConfig;
import com.microsoft.azure.toolkit.ide.guidance.task.GuidanceTaskProvider;
import com.microsoft.azure.toolkit.intellij.appservice.task.CreateFunctionAppTask;
import com.microsoft.azure.toolkit.intellij.appservice.task.CreateWebAppTask;
import com.microsoft.azure.toolkit.intellij.appservice.task.DeployFunctionAppTask;
import com.microsoft.azure.toolkit.intellij.appservice.task.DeployWebAppTask;
import com.microsoft.azure.toolkit.intellij.appservice.task.OpenInBrowserTask;
import com.microsoft.azure.toolkit.intellij.appservice.task.OpenLogStreamingTask;
import com.microsoft.azure.toolkit.intellij.appservice.task.TriggerFunctionTask;

import javax.annotation.Nonnull;

public class AppServiceTaskProvider implements GuidanceTaskProvider {
    @Override
    public Task createTask(@Nonnull TaskConfig config, @Nonnull Context context) {
        final ComponentContext taskContext = new ComponentContext(config, context);
        switch (config.getName()) {
            case "task.webapp.create":
                return new CreateWebAppTask(taskContext);
            case "task.webapp.deploy":
                return new DeployWebAppTask(taskContext);
            case "task.webapp.open_in_browser":
                return new OpenInBrowserTask(taskContext);
            case "task.function.create":
                return new CreateFunctionAppTask(taskContext);
            case "task.function.deploy":
                return new DeployFunctionAppTask(taskContext);
            case "task.app_service.open_log_streaming":
                return new OpenLogStreamingTask(taskContext);
            case "task.function.trigger_function":
                return new TriggerFunctionTask(taskContext);
            default:
                return null;
        }
    }
}
