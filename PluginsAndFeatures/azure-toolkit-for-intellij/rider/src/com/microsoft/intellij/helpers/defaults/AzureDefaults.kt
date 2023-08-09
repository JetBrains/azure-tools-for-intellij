/**
 * Copyright (c) 2018-2023 JetBrains s.r.o.
 *
 * All rights reserved.
 *
 * MIT License
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation files (the "Software"), to deal in the Software without restriction, including without limitation
 * the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and
 * to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of
 * the Software.
 *
 * THE SOFTWARE IS PROVIDED *AS IS*, WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO
 * THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT,
 * TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.microsoft.intellij.helpers.defaults

import com.microsoft.azure.management.appservice.PricingTier
import com.microsoft.azure.management.resources.fluentcore.arm.Region
import com.microsoft.azure.management.sql.DatabaseEdition
import com.microsoft.azure.management.sql.ServiceObjectiveName

object AzureDefaults {

    const val SQL_DATABASE_COLLATION = "SQL_Latin1_General_CP1_CI_AS"

    val databaseEdition: DatabaseEdition = DatabaseEdition.BASIC

    val databaseComputeSize: ServiceObjectiveName = ServiceObjectiveName.BASIC

    val location: Region = Region.US_EAST

    val pricingTier: PricingTier = PricingTier.STANDARD_S1

    object SupportedRegions {
        // Regions where app services are supported
        // https://azure.microsoft.com/en-us/explore/global-infrastructure/products-by-region/?products=app-service
        val AppServices = hashSetOf(
                // South Africa
                "southafricanorth",
                "southafricawest",

                // Asia
                "eastasia",
                "southeastasia",

                // Australia
                "australiacentral",
                "australiacentral2",
                "australiaeast",
                "australiasoutheast",

                // Brazil
                "brazilsouth",
                "brazilsoutheast",

                // Canada
                "canadacentral",
                "canadaeast",

                // China
                "chinaeast",
                "chinaeast2",
                "chinaeast3",
                "chinanorth",
                "chinanorth2",
                "chinanorth3",

                // Europe
                "northeurope",
                "westeurope",

                // France
                "francecentral",
                "francesouth",

                // Germany
                //"germanynorth",
                "germanywestcentral",

                // India
                "centralindia",
                "southindia",
                "westindia",
                "jioindiacentral",
                "jioindiawest",

                // Japan
                "japaneast",
                "japanwest",

                // Korea
                "koreacentral",
                "koreasouth",

                // Norway
                "norwayeast",
                "norwaywest",

                // Poland
                //"polandcentral",

                // Qatar
                "qatarcentral",

                // Sweden
                "swedencentral",
                "swedensouth",

                // Switzerland
                "switzerlandnorth",
                "switzerlandwest",

                // UAE
                "uaecentral",
                "uaenorth",

                // UK
                "uksouth",
                "ukwest",

                // USA
                "centralus",
                "eastus",
                "eastus2",
                "northcentralus",
                "southcentralus",
                "westcentralus",
                "westus",
                "westus2",
                "westus3"
        )

        // Regions where SQL Databasde is supported
        // https://azure.microsoft.com/en-us/explore/global-infrastructure/products-by-region/?products=azure-sql-database
        val SqlDatabase = hashSetOf(
                // South Africa
                "southafricanorth",
                "southafricawest",

                // Asia
                "eastasia",
                "southeastasia",

                // Australia
                "australiacentral",
                "australiacentral2",
                "australiaeast",
                "australiasoutheast",

                // Brazil
                "brazilsouth",
                "brazilsoutheast",

                // Canada
                "canadacentral",
                "canadaeast",

                // China
                "chinaeast",
                "chinaeast2",
                "chinaeast3",
                "chinanorth",
                "chinanorth2",
                "chinanorth3",

                // Europe
                "northeurope",
                "westeurope",

                // France
                "francecentral",
                "francesouth",

                // Germany
                "germanynorth",
                "germanywestcentral",

                // India
                "centralindia",
                "southindia",
                "westindia",
                "jioindiacentral",
                "jioindiawest",

                // Japan
                "japaneast",
                "japanwest",

                // Korea
                "koreacentral",
                "koreasouth",

                // Norway
                "norwayeast",
                "norwaywest",

                // Poland
                "polandcentral",

                // Qatar
                "qatarcentral",

                // Sweden
                "swedencentral",
                "swedensouth",

                // Switzerland
                "switzerlandnorth",
                "switzerlandwest",

                // UAE
                "uaecentral",
                "uaenorth",

                // UK
                "uksouth",
                "ukwest",

                // USA
                "centralus",
                "eastus",
                "eastus2",
                "northcentralus",
                "southcentralus",
                "westcentralus",
                "westus",
                "westus2",
                "westus3"
        )
    }
}
