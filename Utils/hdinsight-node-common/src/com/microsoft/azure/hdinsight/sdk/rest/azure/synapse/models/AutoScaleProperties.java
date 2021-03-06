/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.hdinsight.sdk.rest.azure.synapse.models;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Spark pool auto-scaling properties.
 * Auto-scaling properties of a Big Data pool powered by Apache Spark.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class AutoScaleProperties {
    /**
     * The minimum number of nodes the Big Data pool can support.
     */
    @JsonProperty(value = "minNodeCount")
    private Integer minNodeCount;

    /**
     * Whether automatic scaling is enabled for the Big Data pool.
     */
    @JsonProperty(value = "enabled")
    private Boolean enabled;

    /**
     * The maximum number of nodes the Big Data pool can support.
     */
    @JsonProperty(value = "maxNodeCount")
    private Integer maxNodeCount;

    /**
     * Get the minimum number of nodes the Big Data pool can support.
     *
     * @return the minNodeCount value
     */
    public Integer minNodeCount() {
        return this.minNodeCount;
    }

    /**
     * Set the minimum number of nodes the Big Data pool can support.
     *
     * @param minNodeCount the minNodeCount value to set
     * @return the AutoScaleProperties object itself.
     */
    public AutoScaleProperties withMinNodeCount(Integer minNodeCount) {
        this.minNodeCount = minNodeCount;
        return this;
    }

    /**
     * Get whether automatic scaling is enabled for the Big Data pool.
     *
     * @return the enabled value
     */
    public Boolean enabled() {
        return this.enabled;
    }

    /**
     * Set whether automatic scaling is enabled for the Big Data pool.
     *
     * @param enabled the enabled value to set
     * @return the AutoScaleProperties object itself.
     */
    public AutoScaleProperties withEnabled(Boolean enabled) {
        this.enabled = enabled;
        return this;
    }

    /**
     * Get the maximum number of nodes the Big Data pool can support.
     *
     * @return the maxNodeCount value
     */
    public Integer maxNodeCount() {
        return this.maxNodeCount;
    }

    /**
     * Set the maximum number of nodes the Big Data pool can support.
     *
     * @param maxNodeCount the maxNodeCount value to set
     * @return the AutoScaleProperties object itself.
     */
    public AutoScaleProperties withMaxNodeCount(Integer maxNodeCount) {
        this.maxNodeCount = maxNodeCount;
        return this;
    }

}
