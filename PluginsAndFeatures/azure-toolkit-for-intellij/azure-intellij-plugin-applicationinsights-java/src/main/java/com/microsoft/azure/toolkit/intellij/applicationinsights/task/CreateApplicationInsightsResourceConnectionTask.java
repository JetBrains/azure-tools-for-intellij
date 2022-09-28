package com.microsoft.azure.toolkit.intellij.applicationinsights.task;

import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.project.Project;
import com.microsoft.azure.toolkit.ide.guidance.ComponentContext;
import com.microsoft.azure.toolkit.ide.guidance.Task;
import com.microsoft.azure.toolkit.intellij.applicationinsights.connection.ApplicationInsightsResourceDefinition;
import com.microsoft.azure.toolkit.intellij.connector.*;
import com.microsoft.azure.toolkit.lib.Azure;
import com.microsoft.azure.toolkit.lib.applicationinsights.ApplicationInsight;
import com.microsoft.azure.toolkit.lib.applicationinsights.AzureApplicationInsights;
import com.microsoft.azure.toolkit.lib.common.messager.AzureMessager;
import com.microsoft.azure.toolkit.lib.common.operation.AzureOperation;

import javax.annotation.Nonnull;

// todo: add create resource connection task instead of ai only
// todo: remove duplicate codes with connector dialog
public class CreateApplicationInsightsResourceConnectionTask implements Task {

    private final Project project;
    private final ComponentContext context;

    public CreateApplicationInsightsResourceConnectionTask(@Nonnull final ComponentContext context) {
        this.context = context;
        this.project = context.getProject();
    }

    @Override
    @AzureOperation(name = "guidance.create_application_insights_resource_connection", type = AzureOperation.Type.SERVICE)
    public void execute() throws Exception {
        final Resource resource = getResource();
        final Resource consumer = getModuleConsumer();
        final Connection connection = ConnectionManager.getDefinitionOrDefault(resource.getDefinition(),
                consumer.getDefinition()).define(resource, consumer);
        final ConnectionManager connectionManager = this.project.getService(ConnectionManager.class);
        final ResourceManager resourceManager = ServiceManager.getService(ResourceManager.class);
        if (connection.validate(this.project)) {
            resourceManager.addResource(resource);
            resourceManager.addResource(consumer);
            connectionManager.addConnection(connection);
            final String message = String.format("The connection between %s and %s has been successfully created.",
                    resource.getName(), consumer.getName());
            AzureMessager.getMessager().success(message);
        }
    }

    @Nonnull
    @Override
    public String getName() {
        return "task.ai.create_connection";
    }

    private Resource<ApplicationInsight> getResource() {
        final String applicationInsightsId = (String) context.getParameter("applicationInsightsId");
        final ApplicationInsight applicationInsight = Azure.az(AzureApplicationInsights.class).getById(applicationInsightsId);
        return ApplicationInsightsResourceDefinition.INSTANCE.define(applicationInsight);
    }

    private Resource<String> getModuleConsumer() {
        final Module module = ModuleManager.getInstance(project).getModules()[0];
        return ModuleResource.Definition.IJ_MODULE.define(module.getName());
    }
}
