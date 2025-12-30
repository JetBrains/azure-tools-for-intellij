plugins {
    alias(libs.plugins.kotlin)
    alias(libs.plugins.serialization)
    id("org.jetbrains.intellij.platform.module")
    id("java")
}

repositories {
    mavenCentral()
    mavenLocal()

    intellijPlatform {
        defaultRepositories()
        jetbrainsRuntime()
    }
}

val platformVersion: String by extra

dependencies {
    intellijPlatform {
        rider(platformVersion) {
            useInstaller = false
            useCache = true
        }
        jetbrainsRuntime()
        bundledPlugins(listOf("com.jetbrains.restClient", "Docker", "rider.intellij.plugin.appender"))
    }

    implementation(libs.azureToolkitAuthLib)
    implementation(libs.azureToolkitIdeCommonLib)
    implementation(project(path = ":azure-intellij-plugin-lib"))
    implementation(project(path = ":azure-intellij-plugin-lib-dotnet"))
    implementation(project(path = ":azure-intellij-plugin-appservice"))
    implementation(project(path = ":azure-intellij-resource-connector-lib"))
    implementation(project(path = ":azure-intellij-plugin-resharper-host"))
    implementation(project(path = ":azure-intellij-plugin-storage-dotnet"))
    implementation(libs.azureToolkitAppserviceLib)
    implementation(libs.azureToolkitIdeAppserviceLib)
    implementation(libs.azureToolkitIdeContainerregistryLib)
    implementation(libs.serializationJson)
    implementation(libs.ktorClientContentNegotiation) {
        exclude(group = "org.jetbrains.kotlinx", module = "kotlinx-coroutines-core")
        exclude(group = "org.jetbrains.kotlinx", module = "kotlinx-coroutines-jdk8")
    }
    implementation(libs.ktorSerializationJson) {
        exclude(group = "org.jetbrains.kotlinx", module = "kotlinx-coroutines-core")
        exclude(group = "org.jetbrains.kotlinx", module = "kotlinx-coroutines-jdk8")
        exclude(group = "org.jetbrains.kotlinx", module = "kotlinx-serialization-json")
    }
}

intellijPlatform {
    instrumentCode = false
}
