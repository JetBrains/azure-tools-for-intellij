/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.hdinsight.sdk.rest.azure.datalake.analytics.job.models;

import java.util.List;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeName;

/**
 * Scope job properties used when submitting Scope jobs. (Only for use internally with Scope job type.).
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
@JsonTypeName("Scope")
public class CreateScopeJobProperties extends CreateJobProperties {
    /**
     * The list of resources that are required by the job.
     */
    @JsonProperty(value = "resources")
    private List<ScopeJobResource> resources;

    /**
     * The list of email addresses, separated by semi-colons, to notify when the job reaches a terminal state.
     */
    @JsonProperty(value = "notifier")
    private String notifier;

    /**
     * Get the list of resources that are required by the job.
     *
     * @return the resources value
     */
    public List<ScopeJobResource> resources() {
        return this.resources;
    }

    /**
     * Set the list of resources that are required by the job.
     *
     * @param resources the resources value to set
     * @return the CreateScopeJobProperties object itself.
     */
    public CreateScopeJobProperties withResources(List<ScopeJobResource> resources) {
        this.resources = resources;
        return this;
    }

    /**
     * Get the list of email addresses, separated by semi-colons, to notify when the job reaches a terminal state.
     *
     * @return the notifier value
     */
    public String notifier() {
        return this.notifier;
    }

    /**
     * Set the list of email addresses, separated by semi-colons, to notify when the job reaches a terminal state.
     *
     * @param notifier the notifier value to set
     * @return the CreateScopeJobProperties object itself.
     */
    public CreateScopeJobProperties withNotifier(String notifier) {
        this.notifier = notifier;
        return this;
    }

}
