/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.container.model;

import com.github.dockerjava.api.model.Image;
import com.intellij.openapi.vfs.VirtualFile;
import com.microsoft.azure.toolkit.intellij.common.AzureArtifact;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nonnull;
import java.io.File;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class DockerImage {
    @Builder.Default
    @EqualsAndHashCode.Include
    private boolean isDraft = true;
    @EqualsAndHashCode.Include
    private String repositoryName;
    @EqualsAndHashCode.Include
    private String tagName;
    @EqualsAndHashCode.Include
    private File dockerFile;
    private String imageId;
    private File baseDirectory;
    // todo: check whether we need to add artifact as a field of image
    private AzureArtifact azureArtifact;

    public DockerImage(@Nonnull final String imageAndTag) {
        this.isDraft = false;
        final String[] split = StringUtils.split(imageAndTag, ":");
        this.repositoryName = ArrayUtils.isEmpty(split) ? null : split[0];
        this.tagName = ArrayUtils.getLength(split) < 2 ? null : split[1];
    }

    public DockerImage(@Nonnull final VirtualFile virtualFile) {
        this.isDraft = true;
        this.dockerFile = new File(virtualFile.getPath());
        this.baseDirectory = this.dockerFile.getParentFile();
        this.repositoryName = "image";
        this.tagName = "latest";
    }

    public DockerImage(@Nonnull final  DockerImage value) {
        this.isDraft = value.isDraft;
        this.dockerFile = value.dockerFile;
        this.repositoryName = value.repositoryName;
        this.tagName = value.tagName;
        this.imageId = value.imageId;
    }

    public static List<DockerImage> fromImage(@Nonnull final Image image) {
        if (ArrayUtils.isEmpty(image.getRepoTags())) {
            return Collections.emptyList();
        }
        return Arrays.stream(image.getRepoTags()).map(DockerImage::new).collect(Collectors.toList());
    }

    public String getImageName() {
        return String.format("%s:%s", this.repositoryName, this.tagName);
    }
}
