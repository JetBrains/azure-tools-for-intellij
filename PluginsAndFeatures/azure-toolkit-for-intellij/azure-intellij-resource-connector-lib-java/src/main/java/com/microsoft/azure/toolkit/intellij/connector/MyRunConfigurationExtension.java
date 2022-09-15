package com.microsoft.azure.toolkit.intellij.connector;

import com.intellij.execution.RunConfigurationExtension;
import com.intellij.execution.configurations.JavaParameters;
import com.intellij.execution.configurations.RunConfigurationBase;
import com.intellij.execution.configurations.RunnerSettings;
import com.microsoft.azure.toolkit.lib.common.messager.ExceptionNotification;
import com.microsoft.azure.toolkit.lib.common.operation.AzureOperation;

import javax.annotation.Nonnull;

public class MyRunConfigurationExtension extends RunConfigurationExtension {

    @Override
    @ExceptionNotification
    @AzureOperation(name = "connector.setup_connection_for_configuration", type = AzureOperation.Type.ACTION)
    public <T extends RunConfigurationBase<?>> void updateJavaParameters(@Nonnull T config, @Nonnull JavaParameters params, RunnerSettings s) {
        config.getBeforeRunTasks().stream()
                .filter(t -> t instanceof ConnectionRunnerForRunConfiguration.MyBeforeRunTask)
                .map(t -> (ConnectionRunnerForRunConfiguration.MyBeforeRunTask) t)
                .flatMap(t -> t.getConnections().stream())
                .filter(c -> c instanceof JavaConnection)
                .forEach(c -> ((JavaConnection)c).updateJavaParametersAtRun(config, params));
    }

    @Override
    public boolean isApplicableFor(@Nonnull RunConfigurationBase<?> configuration) {
        return configuration.getBeforeRunTasks().stream().anyMatch(c -> c instanceof ConnectionRunnerForRunConfiguration.MyBeforeRunTask);
    }
}
