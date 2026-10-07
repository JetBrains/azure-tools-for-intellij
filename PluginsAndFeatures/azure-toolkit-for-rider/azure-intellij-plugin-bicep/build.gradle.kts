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
        bundledPlugin("org.jetbrains.plugins.textmate")
    }

    implementation(libs.serializationJson)
}
