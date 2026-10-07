plugins {
    alias(libs.plugins.kotlin)
    id("org.jetbrains.intellij.platform.module")
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

kotlin {
    jvmToolchain(25)
}

dependencies {
    intellijPlatform {
        rider(platformVersion) {
            useInstaller = false
            useCache = true
        }
        jetbrainsRuntime()
        bundledModule("intellij.rider.rdclient.dotnet")
    }

    implementation(project(path = ":azure-intellij-plugin-lib"))
    implementation(project(path = ":azure-intellij-plugin-storage"))
    implementation(project(path = ":azure-intellij-resource-connector-lib"))
    implementation(libs.azureToolkitStorageLib)
    implementation(libs.azureToolkitIdeCommonLib)
    implementation(libs.azureToolkitIdeStorageLib)
}
