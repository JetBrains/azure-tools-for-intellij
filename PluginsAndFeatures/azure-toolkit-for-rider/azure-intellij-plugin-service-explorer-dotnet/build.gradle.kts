/*
 * Copyright 2018-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the MIT license.
 */

plugins {
    id("azure.kotlin-module")
}

dependencies {
    implementation(project(path = ":azure-intellij-plugin-core"))
    implementation(project(path = ":azure-intellij-plugin-lib"))
    implementation(project(path = ":azure-intellij-plugin-service-explorer"))
}
