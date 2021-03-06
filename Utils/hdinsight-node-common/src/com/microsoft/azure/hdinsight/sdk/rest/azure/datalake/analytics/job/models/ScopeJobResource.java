/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.hdinsight.sdk.rest.azure.datalake.analytics.job.models;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * The Scope job resources. (Only for use internally with Scope job type.).
 */
public class ScopeJobResource {
    /**
     * The name of the resource.
     */
    @JsonProperty(value = "name")
    private String name;

    /**
     * The path to the resource.
     */
    @JsonProperty(value = "path")
    private String path;

    /**
     * Get the name of the resource.
     *
     * @return the name value
     */
    public String name() {
        return this.name;
    }

    /**
     * Set the name of the resource.
     *
     * @param name the name value to set
     * @return the ScopeJobResource object itself.
     */
    public ScopeJobResource withName(String name) {
        this.name = name;
        return this;
    }

    /**
     * Get the path to the resource.
     *
     * @return the path value
     */
    public String path() {
        return this.path;
    }

    /**
     * Set the path to the resource.
     *
     * @param path the path value to set
     * @return the ScopeJobResource object itself.
     */
    public ScopeJobResource withPath(String path) {
        this.path = path;
        return this;
    }

}
