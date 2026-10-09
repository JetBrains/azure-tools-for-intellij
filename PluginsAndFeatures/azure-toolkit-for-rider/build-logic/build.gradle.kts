/*
 * Copyright 2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the MIT license.
 */

plugins {
    `kotlin-dsl`
}

repositories {
    gradlePluginPortal()
    mavenCentral()
}

dependencies {
    implementation(libs.kotlinGradlePlugin)
    implementation(libs.intellijPlatformGradlePlugin)
    implementation(libs.aspectjGradlePlugin)
}
