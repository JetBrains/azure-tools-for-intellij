/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.hdinsight.sdk.rest.azure.datalake.analytics.accounts.models;

import java.util.Collection;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.microsoft.rest.ExpandableStringEnum;

/**
 * Defines values for AADObjectType.
 */
public final class AADObjectType extends ExpandableStringEnum<AADObjectType> {
    /** Static value User for AADObjectType. */
    public static final AADObjectType USER = fromString("User");

    /** Static value Group for AADObjectType. */
    public static final AADObjectType GROUP = fromString("Group");

    /** Static value ServicePrincipal for AADObjectType. */
    public static final AADObjectType SERVICE_PRINCIPAL = fromString("ServicePrincipal");

    /**
     * Creates or finds a AADObjectType from its string representation.
     * @param name a name to look for
     * @return the corresponding AADObjectType
     */
    @JsonCreator
    public static AADObjectType fromString(String name) {
        return fromString(name, AADObjectType.class);
    }

    /**
     * @return known AADObjectType values
     */
    public static Collection<AADObjectType> values() {
        return values(AADObjectType.class);
    }
}
