/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.containerregistry.pushimage;

import com.intellij.openapi.project.Project;
import com.microsoft.azure.toolkit.intellij.common.RunProcessHandler;
import com.microsoft.azure.toolkit.intellij.container.model.DockerImage;
import com.microsoft.azure.toolkit.intellij.containerregistry.ContainerService;
import com.microsoft.azure.toolkit.intellij.legacy.common.AzureRunProfileState;
import com.microsoft.azure.toolkit.lib.common.operation.AzureOperation;
import com.microsoft.azure.toolkit.lib.common.operation.OperationContext;
import com.microsoft.azuretools.core.mvp.model.container.pojo.PushImageRunModel;
import com.microsoft.azuretools.telemetry.TelemetryConstants;
import com.microsoft.azuretools.telemetrywrapper.Operation;
import com.microsoft.azuretools.telemetrywrapper.TelemetryManager;
import com.microsoft.intellij.util.MavenRunTaskUtil;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nonnull;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;

public class PushImageRunState extends AzureRunProfileState<String> {
    private final PushImageRunModel dataModel;
    private final PushImageRunConfiguration configuration;

    public PushImageRunState(Project project, PushImageRunConfiguration configuration) {
        super(project);
        this.configuration = configuration;
        this.dataModel = configuration.getModel();
    }

    @Override
    @AzureOperation(name = "platform/docker.push_image.registry", params = {"nameFromResourceId(this.dataModel.getContainerRegistryId())"})
    public String executeSteps(@Nonnull RunProcessHandler processHandler, @Nonnull Operation operation) throws Exception {
        OperationContext.current().setMessager(getProcessHandlerMessenger());
        return ContainerService.getInstance().pushDockerImage(configuration);
    }

    @Nonnull
    @Override
    protected Operation createOperation() {
        return TelemetryManager.createOperation(TelemetryConstants.ACR, TelemetryConstants.ACR_PUSHIMAGE);
    }

    @Override
    protected void onSuccess(String registry, @Nonnull RunProcessHandler processHandler) {
        final String imageName = Optional.ofNullable(configuration.getDockerImageConfiguration()).map(DockerImage::getImageName).orElse(null);
        processHandler.setText(String.format("Image (%s) has been pushed to registry (%s)", imageName, registry));
        processHandler.notifyComplete();
    }

    @Override
    protected Map<String, String> getTelemetryMap() {
        final String fileType = dataModel.getTargetName() == null ? StringUtils.EMPTY : MavenRunTaskUtil.getFileType(dataModel.getTargetName());
        return Collections.singletonMap(TelemetryConstants.FILETYPE, fileType);
    }
}
