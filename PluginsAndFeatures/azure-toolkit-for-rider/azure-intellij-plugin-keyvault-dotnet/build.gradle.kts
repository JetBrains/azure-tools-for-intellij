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
        bundledPlugins("com.intellij.properties", "org.jetbrains.plugins.yaml")
    }

    implementation(project(path = ":azure-intellij-plugin-lib"))
    implementation(project(path = ":azure-intellij-plugin-keyvault"))
    implementation(project(path = ":azure-intellij-resource-connector-lib"))
    implementation(libs.azureToolkitKeyvaultLib)
    implementation(libs.azureToolkitIdeCommonLib)
    implementation(libs.azureToolkitIdeKeyvaultLib)
}
