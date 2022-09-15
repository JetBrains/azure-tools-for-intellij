/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.connector;

import com.intellij.execution.configurations.RunConfiguration;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.project.Project;
import com.microsoft.azure.toolkit.intellij.common.runconfig.IWebAppRunConfiguration;
import com.microsoft.azure.toolkit.lib.common.messager.AzureMessager;
import com.microsoft.azure.toolkit.lib.common.operation.AzureOperation;
import lombok.*;
import org.apache.commons.lang3.StringUtils;
import org.jdom.Element;

import javax.annotation.Nonnull;
import java.util.*;
import java.util.stream.Collectors;

/**
 * the <b>{@code resource connection}</b>
 *
 * @param <R> type of the resource consumed by {@link C}
 * @param <C> type of the consumer consuming {@link R},
 *            it can only be {@link ModuleResource} for now({@code v3.52.0})
 * @since 3.52.0
 */
@Getter
@RequiredArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class Connection<R, C> {
    public static final String ENV_PREFIX = "%ENV_PREFIX%";

    @Nonnull
    @EqualsAndHashCode.Include
    protected final Resource<R> resource;

    @Nonnull
    @EqualsAndHashCode.Include
    protected final Resource<C> consumer;

    @Nonnull
    @EqualsAndHashCode.Include
    protected final ConnectionDefinition<R, C> definition;

    @Setter
    @Getter(AccessLevel.NONE)
    private String envPrefix;

    private Map<String, String> env = new HashMap<>();

    /**
     * is this connection applicable for the specified {@code configuration}.<br>
     * - the {@code Connect Azure Resource} before run task will take effect if
     * applicable: the {@link #prepareBeforeRun}
     * will be called.
     *
     * @return true if this connection should intervene the specified {@code configuration}.
     */
    public boolean isApplicableFor(@Nonnull RunConfiguration configuration) {
        return configuration instanceof IWebAppRunConfiguration;
    }

    /**
     * do some preparation in the {@code Connect Azure Resource} before run task
     * of the {@code configuration}<br>
     */
    @AzureOperation(name = "connector.prepare_before_run", type = AzureOperation.Type.ACTION)
    public boolean prepareBeforeRun(@Nonnull RunConfiguration configuration, DataContext dataContext) {
        try {
            this.env = this.resource.initEnv(configuration.getProject()).entrySet().stream()
                .collect(Collectors.toMap(
                    e -> e.getKey().replaceAll(Connection.ENV_PREFIX, this.getEnvPrefix()),
                    Map.Entry::getValue));
            if (configuration instanceof IWebAppRunConfiguration) { // set envs for remote deploy
                final IWebAppRunConfiguration webAppConfiguration = (IWebAppRunConfiguration) configuration;
                final Map<String, String> settings = Optional.ofNullable(webAppConfiguration.getApplicationSettings()).orElse(new HashMap<>());
                settings.putAll(this.env);
                webAppConfiguration.setApplicationSettings(settings);
            }

            return true;
        } catch (final Throwable e) {
            AzureMessager.getMessager().error(e);
            return false;
        }
    }

    public boolean isEnvironmentSet() {
        return Objects.nonNull(this.env);
    }

    public Set<Map.Entry<String, String>> getEnvironmentEntries() {
        return env.entrySet();
    }

    public String getEnvPrefix() {
        if (StringUtils.isBlank(this.envPrefix)) {
            return this.definition.getResourceDefinition().getDefaultEnvPrefix();
        }
        return this.envPrefix;
    }

    public void write(Element connectionEle) {
        this.getDefinition().write(connectionEle, this);
    }

    public boolean validate(Project project) {
        return this.getDefinition().validate(this, project);
    }
}
