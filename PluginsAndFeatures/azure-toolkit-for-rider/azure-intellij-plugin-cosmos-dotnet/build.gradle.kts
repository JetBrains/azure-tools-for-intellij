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
        bundledPlugin("com.intellij.database")
    }

    implementation(project(path = ":azure-intellij-plugin-lib"))
    implementation(project(path = ":azure-intellij-resource-connector-lib"))
    implementation(project(path = ":azure-intellij-plugin-cosmos"))
    implementation(libs.azureToolkitCosmosLib)
    implementation(libs.azureToolkitIdeCosmosLib)
    implementation(libs.azureToolkitIdentityLib)
    implementation(libs.azureToolkitIdeCommonLib)
}
