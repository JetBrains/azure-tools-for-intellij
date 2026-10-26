/*
 * Copyright 2018-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the MIT license.
 */

import org.gradle.api.tasks.testing.logging.TestExceptionFormat
import org.jetbrains.changelog.Changelog
import org.jetbrains.intellij.platform.gradle.IntelliJPlatformType
import org.jetbrains.intellij.platform.gradle.TestFrameworkType

plugins {
    alias(libs.plugins.kotlin)
    alias(libs.plugins.intelliJPlatform)
    alias(libs.plugins.serialization)
    alias(libs.plugins.changelog)
}

group = providers.gradleProperty("pluginGroup").get()
version = providers.gradleProperty("pluginVersion").get()

val platformVersion by extra { providers.gradleProperty("platformVersion").get() }

kotlin {
    jvmToolchain(25)
}

repositories {
    mavenCentral()
    mavenLocal()

    // IntelliJ Platform Gradle Plugin Repositories Extension - read more: https://plugins.jetbrains.com/docs/intellij/tools-intellij-platform-gradle-plugin-repositories-extension.html
    intellijPlatform {
        defaultRepositories()
    }
}

// Dependencies are managed with Gradle version catalog - read more: https://docs.gradle.org/current/userguide/platforms.html#sub:version-catalog
dependencies {
    implementation(platform(libs.nettyBom))
    implementation(libs.azureToolkitLibs)
    implementation(libs.azureToolkitIdeLibs)
    implementation(libs.azureToolkitHdinsightLibs)

    implementation(libs.azureToolkitCommonLib)
    implementation(libs.azureToolkitIdeCommonLib)

    testImplementation(libs.opentest4j)
    testImplementation(libs.junit)

    // IntelliJ Platform Gradle Plugin Dependencies Extension - read more: https://plugins.jetbrains.com/docs/intellij/tools-intellij-platform-gradle-plugin-dependencies-extension.html
    intellijPlatform {
        pluginComposedModule(implementation(project(path = ":azure-intellij-plugin-core")))
        pluginComposedModule(implementation(project(path = ":azure-intellij-plugin-lib")))
        pluginComposedModule(implementation(project(path = ":azure-intellij-plugin-lib-dotnet")))
        pluginComposedModule(implementation(project(path = ":azure-intellij-plugin-service-explorer")))
        pluginComposedModule(implementation(project(path = ":azure-intellij-plugin-service-explorer-dotnet")))
        pluginComposedModule(implementation(project(path = ":azure-intellij-resource-connector-lib")))
        pluginComposedModule(implementation(project(path = ":azure-intellij-plugin-arm")))
        pluginComposedModule(implementation(project(path = ":azure-intellij-plugin-appservice")))
        pluginComposedModule(implementation(project(path = ":azure-intellij-plugin-appservice-dotnet")))
        pluginComposedModule(implementation(project(path = ":azure-intellij-plugin-database")))
        pluginComposedModule(implementation(project(path = ":azure-intellij-plugin-database-dotnet")))
        pluginComposedModule(implementation(project(path = ":azure-intellij-plugin-cloud-shell")))
        pluginComposedModule(implementation(project(path = ":azure-intellij-plugin-redis")))
        pluginComposedModule(implementation(project(path = ":azure-intellij-plugin-redis-dotnet")))
        pluginComposedModule(implementation(project(path = ":azure-intellij-plugin-storage")))
        pluginComposedModule(implementation(project(path = ":azure-intellij-plugin-storage-dotnet")))
        pluginComposedModule(implementation(project(path = ":azure-intellij-plugin-keyvault")))
        pluginComposedModule(implementation(project(path = ":azure-intellij-plugin-keyvault-dotnet")))
        pluginComposedModule(implementation(project(path = ":azure-intellij-plugin-servicebus")))
        pluginComposedModule(implementation(project(path = ":azure-intellij-plugin-eventhubs")))
        pluginComposedModule(implementation(project(path = ":azure-intellij-plugin-vm")))
        pluginComposedModule(implementation(project(path = ":azure-intellij-plugin-cosmos")))
        pluginComposedModule(implementation(project(path = ":azure-intellij-plugin-cosmos-dotnet")))
        pluginComposedModule(implementation(project(path = ":azure-intellij-plugin-bicep")))

        rider(platformVersion) {
            useInstaller = false
            useCache = true
        }
        testFramework(TestFrameworkType.Bundled)
    }
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

// Configure IntelliJ Platform Gradle Plugin - read more: https://plugins.jetbrains.com/docs/intellij/tools-intellij-platform-gradle-plugin-extension.html
intellijPlatform {
    pluginConfiguration {
        name = providers.gradleProperty("pluginName")
        version = providers.gradleProperty("pluginVersion")

        val changelog = project.changelog // local variable for configuration cache compatibility
        // Get the latest available change notes from the changelog file
        changeNotes = providers.gradleProperty("pluginVersion").map { pluginVersion ->
            with(changelog) {
                renderItem(
                    (getOrNull(pluginVersion) ?: getUnreleased())
                        .withHeader(false)
                        .withEmptySections(false),
                    Changelog.OutputType.HTML,
                )
            }
        }

        ideaVersion {
            sinceBuild = providers.gradleProperty("pluginSinceBuild")
        }
    }

    buildSearchableOptions = false

    signing {
        certificateChain = providers.environmentVariable("CERTIFICATE_CHAIN")
        privateKey = providers.environmentVariable("PRIVATE_KEY")
        password = providers.environmentVariable("PRIVATE_KEY_PASSWORD")
    }

    publishing {
        token = providers.environmentVariable("PUBLISH_TOKEN")
        // The pluginVersion is based on the SemVer (https://semver.org) and supports pre-release labels, like 2.1.7-alpha.3
        // Specify pre-release label to publish the plugin in a custom Release Channel automatically. Read more:
        // https://plugins.jetbrains.com/docs/intellij/deployment.html#specifying-a-release-channel
        channels = providers.gradleProperty("pluginVersion")
            .map { listOf(it.substringAfter('-', "").substringBefore('.').ifEmpty { "default" }) }
        hidden = true
    }

    pluginVerification {
        ides {
            create(
                IntelliJPlatformType.Rider,
                providers.gradleProperty("pluginVerificationIdeVersion").get()
            ) { useInstaller = false }
        }
    }
}

// Configure Gradle Changelog Plugin - read more: https://github.com/JetBrains/gradle-changelog-plugin
changelog {
    groups.empty()
    repositoryUrl = providers.gradleProperty("pluginRepositoryUrl")
}

tasks {
    wrapper {
        gradleVersion = providers.gradleProperty("gradleVersion").get()
    }

    publishPlugin {
        dependsOn(patchChangelog)
    }

    test {
        useJUnitPlatform()
        testLogging {
            showStandardStreams = true
            exceptionFormat = TestExceptionFormat.FULL
        }
        environment["LOCAL_ENV_RUN"] = "true"
    }
}
