/*
 * Copyright 2018-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the MIT license.
 */

plugins {
    id("azure.kotlin-module")
}

dependencies {
    intellijPlatform {
        bundledPlugin("com.jetbrains.restClient")
    }

    implementation(libs.azureToolkitLibs)
    implementation(libs.azureToolkitIdeLibs)
    implementation(libs.azureToolkitHdinsightLibs)
    implementation(libs.azureToolkitCommonLib)
    implementation(libs.azureToolkitIdeCommonLib)

    implementation(project(path = ":azure-intellij-plugin-lib"))
}

configurations {
    implementation { exclude(module = "slf4j-api") }
    implementation { exclude(module = "log4j") }
    implementation { exclude(module = "stax-api") }
    implementation { exclude(module = "groovy-xml") }
    implementation { exclude(module = "jna") }
    implementation { exclude(module = "xpp3") }
    implementation { exclude(module = "pull-parser") }
    implementation { exclude(module = "xsdlib") }
}
