package com.microsoft.azure.toolkit.intellij.applicationinsights;

import com.microsoft.azure.toolkit.ide.guidance.ComponentContext;
import com.microsoft.azure.toolkit.ide.guidance.Context;
import com.microsoft.azure.toolkit.ide.guidance.Task;
import com.microsoft.azure.toolkit.ide.guidance.config.TaskConfig;
import com.microsoft.azure.toolkit.ide.guidance.task.GuidanceTaskProvider;
import com.microsoft.azure.toolkit.intellij.applicationinsights.task.CreateApplicationInsightsResourceConnectionTask;
import com.microsoft.azure.toolkit.intellij.applicationinsights.task.CreateApplicationInsightsTask;
import com.microsoft.azure.toolkit.intellij.applicationinsights.task.OpenLiveMetricsTask;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class ApplicationInsightsTaskProvider implements GuidanceTaskProvider {
    @Nullable
    @Override
    public Task createTask(@Nonnull TaskConfig config, @Nonnull Context context) {
        final ComponentContext taskContext = new ComponentContext(config, context);
        switch (config.getName()) {
            case "task.application_insights.create":
                return new CreateApplicationInsightsTask(taskContext);
            case "task.application_insights.create_connector":
                return new CreateApplicationInsightsResourceConnectionTask(taskContext);
            case "task.application_insights.live_metrics":
                return new OpenLiveMetricsTask(taskContext);
            default:
                return null;
        }
    }
}
