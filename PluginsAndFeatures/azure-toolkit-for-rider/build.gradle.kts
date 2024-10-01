/*
 * Copyright 2018-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the MIT license.
 */

import com.jetbrains.plugin.structure.base.utils.isFile
import org.jetbrains.changelog.Changelog
import org.jetbrains.changelog.markdownToHTML
import org.jetbrains.intellij.platform.gradle.Constants
import org.jetbrains.intellij.platform.gradle.TestFrameworkType
import org.jetbrains.intellij.platform.gradle.tasks.PrepareSandboxTask
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    alias(libs.plugins.kotlin)
    alias(libs.plugins.intelliJPlatform)
    alias(libs.plugins.serialization)
    alias(libs.plugins.changelog)
    alias(libs.plugins.qodana)
}

group = providers.gradleProperty("pluginGroup").get()
version = providers.gradleProperty("pluginVersion").get()

val platformVersion by extra { providers.gradleProperty("platformVersion").get() }

// Set the JVM language level used to build the project.
kotlin {
    jvmToolchain(17)
}

repositories {
    mavenCentral()
    mavenLocal()

    // IntelliJ Platform Gradle Plugin Repositories Extension - read more: https://plugins.jetbrains.com/docs/intellij/tools-intellij-platform-gradle-plugin-repositories-extension.html
    intellijPlatform {
        defaultRepositories()
        jetbrainsRuntime()
    }
}

// Dependencies are managed with Gradle version catalog - read more: https://docs.gradle.org/current/userguide/platforms.html#sub:version-catalog
dependencies {
    implementation(libs.azureToolkitLibs)
    implementation(libs.azureToolkitIdeLibs)
    implementation(libs.azureToolkitHdinsightLibs)

    implementation(libs.azureToolkitCommonLib)
    implementation(libs.azureToolkitIdeCommonLib)

    implementation(project(path = ":azure-intellij-plugin-lib"))
    implementation(project(path = ":azure-intellij-plugin-lib-dotnet"))
    implementation(project(path = ":azure-intellij-plugin-service-explorer"))
    implementation(project(path = ":azure-intellij-resource-connector-lib"))
    implementation(project(path = ":azure-intellij-plugin-guidance"))
    implementation(project(path = ":azure-intellij-plugin-arm"))
    implementation(project(path = ":azure-intellij-plugin-monitor"))
    implementation(project(path = ":azure-intellij-plugin-appservice"))
    implementation(project(path = ":azure-intellij-plugin-appservice-dotnet"))
    implementation(project(path = ":azure-intellij-plugin-database"))
    implementation(project(path = ":azure-intellij-plugin-database-dotnet"))
    implementation(project(path = ":azure-intellij-plugin-cloud-shell"))
    implementation(project(path = ":azure-intellij-plugin-redis"))
    implementation(project(path = ":azure-intellij-plugin-redis-dotnet"))
    implementation(project(path = ":azure-intellij-plugin-storage"))
    implementation(project(path = ":azure-intellij-plugin-storage-dotnet"))
    implementation(project(path = ":azure-intellij-plugin-keyvault"))
    implementation(project(path = ":azure-intellij-plugin-keyvault-dotnet"))
    implementation(project(path = ":azure-intellij-plugin-servicebus"))
    implementation(project(path = ":azure-intellij-plugin-eventhubs"))
    implementation(project(path = ":azure-intellij-plugin-vm"))

    testImplementation(libs.opentest4j)

    // IntelliJ Platform Gradle Plugin Dependencies Extension - read more: https://plugins.jetbrains.com/docs/intellij/tools-intellij-platform-gradle-plugin-dependencies-extension.html
    intellijPlatform {
        rider(platformVersion, false)
        jetbrainsRuntime()
        bundledPlugins(listOf("com.jetbrains.restClient"))
        instrumentationTools()
        pluginVerifier()
        zipSigner()
        testFramework(TestFrameworkType.Bundled)
    }
}

configurations {
    implementation { exclude(module = "slf4j-api") }
    implementation { exclude(module = "log4j") }
    implementation { exclude(module = "stax-api") }
    implementation { exclude(module = "groovy-xml") }
    implementation { exclude(module = "groovy-templates") }
    implementation { exclude(module = "jna") }
    implementation { exclude(module = "xpp3") }
    implementation { exclude(module = "pull-parser") }
    implementation { exclude(module = "xsdlib") }
}

// Configure IntelliJ Platform Gradle Plugin - read more: https://plugins.jetbrains.com/docs/intellij/tools-intellij-platform-gradle-plugin-extension.html
intellijPlatform {
    pluginConfiguration {
        version = providers.gradleProperty("pluginVersion")

        // Extract the <!-- Plugin description --> section from README.md and provide for the plugin's manifest
        description = providers.fileContents(layout.projectDirectory.file("README.md")).asText.map {
            val start = "<!-- Plugin description -->"
            val end = "<!-- Plugin description end -->"

            with(it.lines()) {
                if (!containsAll(listOf(start, end))) {
                    throw GradleException("Plugin description section not found in README.md:\n$start ... $end")
                }
                subList(indexOf(start) + 1, indexOf(end)).joinToString("\n").let(::markdownToHTML)
            }
        }

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
            untilBuild = providers.gradleProperty("pluginUntilBuild")
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
            recommended()
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

    val rdGen = ":protocol:rdgen"

    val dotnetBuildConfiguration = providers.gradleProperty("dotnetBuildConfiguration").get()
    val compileDotNet by registering {
        dependsOn(rdGen)
        doLast {
            exec {
                executable("dotnet")
                args("build", "-c", dotnetBuildConfiguration, "/clp:ErrorsOnly", "ReSharper.Azure.sln")
            }
        }
    }

    withType<KotlinCompile> {
        dependsOn(rdGen)
    }

    buildPlugin {
        dependsOn(compileDotNet)
    }

    withType<PrepareSandboxTask> {
        dependsOn(compileDotNet)

        val dotnetOutputFolder = file("$projectDir/src/dotnet/ReSharper.Azure")

        val dllFiles = listOf(
            "$dotnetOutputFolder/Azure.Project/bin/$dotnetBuildConfiguration/JetBrains.ReSharper.Azure.Project.dll",
            "$dotnetOutputFolder/Azure.Project/bin/$dotnetBuildConfiguration/JetBrains.ReSharper.Azure.Project.pdb",
            "$dotnetOutputFolder/Azure.Psi/bin/$dotnetBuildConfiguration/JetBrains.ReSharper.Azure.Psi.dll",
            "$dotnetOutputFolder/Azure.Psi/bin/$dotnetBuildConfiguration/JetBrains.ReSharper.Azure.Psi.pdb",
            "$dotnetOutputFolder/Azure.Intellisense/bin/$dotnetBuildConfiguration/JetBrains.ReSharper.Azure.Intellisense.dll",
            "$dotnetOutputFolder/Azure.Intellisense/bin/$dotnetBuildConfiguration/JetBrains.ReSharper.Azure.Intellisense.pdb",
            "$dotnetOutputFolder/Azure.Daemon/bin/$dotnetBuildConfiguration/JetBrains.ReSharper.Azure.Daemon.dll",
            "$dotnetOutputFolder/Azure.Daemon/bin/$dotnetBuildConfiguration/JetBrains.ReSharper.Azure.Daemon.pdb",
            "$dotnetOutputFolder/Azure.Daemon/bin/$dotnetBuildConfiguration/NCrontab.Signed.dll",
            "$dotnetOutputFolder/Azure.Daemon/bin/$dotnetBuildConfiguration/CronExpressionDescriptor.dll"
        )

        for (f in dllFiles) {
            from(f) { into("${rootProject.name}/dotnet") }
        }

        val dotnetExtensionsFolder =
            file("$projectDir/src/main/resources/dotnet/Extensions/com.intellij.resharper.azure")

        from(dotnetExtensionsFolder) { into("${rootProject.name}/dotnet/Extensions/com.intellij.resharper.azure") }

        doLast {
            for (f in dllFiles) {
                val file = file(f)
                if (!file.exists()) throw RuntimeException("File \"$file\" does not exist")
            }
        }
    }

    publishPlugin {
        dependsOn(patchChangelog)
    }
}

val riderModel: Configuration by configurations.creating {
    isCanBeConsumed = true
    isCanBeResolved = false
}

artifacts {
    add(riderModel.name, provider {
        intellijPlatform.platformPath.resolve("lib/rd/rider-model.jar").also {
            check(it.isFile) {
                "rider-model.jar is not found at $riderModel"
            }
        }
    }) {
        builtBy(Constants.Tasks.INITIALIZE_INTELLIJ_PLATFORM_PLUGIN)
    }
}
