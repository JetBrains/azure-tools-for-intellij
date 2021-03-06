/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.hdinsight.sdk.rest.azure.datalake.analytics.job.models;

import java.util.Map;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * The parameters used to submit a new Data Lake Analytics Scope job. (Only for use internally with Scope job type.).
 */
public class CreateScopeJobParameters extends CreateJobParameters {
    /**
     * The key-value pairs used to add additional metadata to the job information.
     */
    @JsonProperty(value = "tags")
    private Map<String, String> tags;

    /**
     * Get the key-value pairs used to add additional metadata to the job information.
     *
     * @return the tags value
     */
    public Map<String, String> tags() {
        return this.tags;
    }

    /**
     * Set the key-value pairs used to add additional metadata to the job information.
     *
     * @param tags the tags value to set
     * @return the CreateScopeJobParameters object itself.
     */
    public CreateScopeJobParameters withTags(Map<String, String> tags) {
        this.tags = tags;
        return this;
    }

}
