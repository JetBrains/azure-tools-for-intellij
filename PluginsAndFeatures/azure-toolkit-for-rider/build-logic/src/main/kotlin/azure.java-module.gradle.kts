/*
 * Copyright 2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the MIT license.
 */

import org.gradle.api.artifacts.VersionCatalogsExtension

plugins {
    id("java")
    id("io.freefair.aspectj.post-compile-weaving")
    id("azure.platform-module")
}

val libs = extensions.getByType<VersionCatalogsExtension>().named("libs")

dependencies {
    implementation(libs.findLibrary("azureToolkitLibs").get())
    implementation(libs.findLibrary("azureToolkitIdeLibs").get())
    implementation(libs.findLibrary("azureToolkitHdinsightLibs").get())

    compileOnly(libs.findLibrary("lombok").get())
    compileOnly(libs.findLibrary("annotations").get())
    annotationProcessor(libs.findLibrary("lombok").get())
    implementation(libs.findLibrary("azureToolkitCommonLib").get())
    aspect(libs.findLibrary("azureToolkitCommonLib").get())
    implementation(libs.findLibrary("aspectjRuntime").get())
}

configurations {
    implementation {
        listOf(
            "slf4j-api",
            "log4j",
            "stax-api",
            "groovy-xml",
            "jna",
            "xpp3",
            "pull-parser",
            "xsdlib"
        ).forEach { exclude(module = it) }
    }
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(25)
    }
}

tasks {
    compileJava {
        options.release.set(25)
    }

    processResources {
        duplicatesStrategy = DuplicatesStrategy.WARN
    }
}
