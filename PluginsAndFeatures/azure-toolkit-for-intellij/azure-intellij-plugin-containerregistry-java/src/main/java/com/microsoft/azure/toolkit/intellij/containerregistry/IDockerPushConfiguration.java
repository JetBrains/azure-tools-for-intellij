/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.containerregistry;

public interface IDockerPushConfiguration extends IDockerConfiguration {
    String getContainerRegistryId();

    String getFinalRepositoryName();

    String getFinalTagName();

    default String getFinalImageName() {
        return getFinalRepositoryName() + ":" + getFinalTagName();
    }
}
