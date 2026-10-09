/*
 * Copyright 2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the MIT license.
 */

plugins {
    id("org.jetbrains.intellij.platform.module")
}

repositories {
    mavenCentral()
    mavenLocal()

    intellijPlatform {
        defaultRepositories()
    }
}

dependencies {
    intellijPlatform {
        rider(providers.gradleProperty("platformVersion")) {
            useInstaller = false
            useCache = true
        }
    }
}
