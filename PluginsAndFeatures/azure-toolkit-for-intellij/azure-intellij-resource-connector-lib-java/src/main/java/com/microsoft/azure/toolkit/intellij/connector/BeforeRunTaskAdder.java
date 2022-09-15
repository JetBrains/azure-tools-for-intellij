package com.microsoft.azure.toolkit.intellij.connector;

import com.intellij.execution.BeforeRunTask;
import com.intellij.execution.RunManagerEx;
import com.intellij.execution.RunManagerListener;
import com.intellij.execution.RunnerAndConfigurationSettings;
import com.intellij.execution.configurations.RunConfiguration;
import com.intellij.execution.impl.ConfigurationSettingsEditorWrapper;
import com.intellij.openapi.project.Project;
import com.microsoft.azure.toolkit.intellij.common.runconfig.IWebAppRunConfiguration;
import com.microsoft.azure.toolkit.lib.common.messager.ExceptionNotification;
import com.microsoft.azure.toolkit.lib.common.operation.AzureOperation;
import com.microsoft.intellij.util.BuildArtifactBeforeRunTaskUtils;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class BeforeRunTaskAdder implements RunManagerListener, ConnectionTopics.ConnectionChanged, IWebAppRunConfiguration.ModuleChangedListener {
    @Override
    @ExceptionNotification
    @AzureOperation(name = "connector.update_connection_task", type = AzureOperation.Type.ACTION)
    public void runConfigurationAdded(@Nonnull RunnerAndConfigurationSettings settings) {
        final RunConfiguration config = settings.getConfiguration();
        final List<Connection<?, ?>> connections = config.getProject().getService(ConnectionManager.class).getConnections();
        final List<BeforeRunTask<?>> tasks = config.getBeforeRunTasks();
        if (connections.stream().anyMatch(c -> c.isApplicableFor(config)) && tasks.stream().noneMatch(t -> t instanceof ConnectionRunnerForRunConfiguration.MyBeforeRunTask)) {
            config.getBeforeRunTasks().add(new ConnectionRunnerForRunConfiguration.MyBeforeRunTask(config));
        }
    }

    @Override
    public void runConfigurationChanged(@NotNull RunnerAndConfigurationSettings settings) {
        this.artifactMayChanged(settings.getConfiguration(), null);
    }

    @Override
    @ExceptionNotification
    @AzureOperation(name = "connector.update_connection_task", type = AzureOperation.Type.ACTION)
    public void connectionChanged(Project project, Connection<?, ?> connection, ConnectionTopics.Action change) {
        final RunManagerEx rm = RunManagerEx.getInstanceEx(project);
        final List<RunConfiguration> configurations = rm.getAllConfigurationsList();
        for (final RunConfiguration config : configurations) {
            final List<BeforeRunTask<?>> tasks = config.getBeforeRunTasks();
            if (change == ConnectionTopics.Action.ADD) {
                if (connection.isApplicableFor(config) && tasks.stream().noneMatch(t -> t instanceof ConnectionRunnerForRunConfiguration.MyBeforeRunTask)) {
                    tasks.add(new ConnectionRunnerForRunConfiguration.MyBeforeRunTask(config));
                }
            } else {
                final List<Connection<?, ?>> connections = config.getProject().getService(ConnectionManager.class).getConnections();
                if (connections.stream().noneMatch(c -> c.isApplicableFor(config))) {
                    tasks.removeIf(t -> t instanceof ConnectionRunnerForRunConfiguration.MyBeforeRunTask);
                }
            }
        }
    }

    @Override
    @ExceptionNotification
    @AzureOperation(name = "connector.update_connection_task", type = AzureOperation.Type.ACTION)
    public void artifactMayChanged(@Nonnull RunConfiguration config, @Nullable ConfigurationSettingsEditorWrapper editor) {
        final List<Connection<?, ?>> connections = config.getProject().getService(ConnectionManager.class).getConnections();
        final List<BeforeRunTask<?>> tasks = config.getBeforeRunTasks();
        Optional.ofNullable(editor).ifPresent(e -> BuildArtifactBeforeRunTaskUtils.removeTasks(e, (t) -> t instanceof ConnectionRunnerForRunConfiguration.MyBeforeRunTask));
        tasks.removeIf(t -> t instanceof ConnectionRunnerForRunConfiguration.MyBeforeRunTask);
        if (connections.stream().anyMatch(c -> c.isApplicableFor(config))) {
            final List<BeforeRunTask> newTasks = new ArrayList<>(tasks);
            final ConnectionRunnerForRunConfiguration.MyBeforeRunTask task = new ConnectionRunnerForRunConfiguration.MyBeforeRunTask(config);
            newTasks.add(task);
            RunManagerEx.getInstanceEx(config.getProject()).setBeforeRunTasks(config, newTasks);
            Optional.ofNullable(editor).ifPresent(e -> e.addBeforeLaunchStep(task));
        }
    }
}
