plugins {
    alias(libs.plugins.kotlin)
    alias(libs.plugins.serialization)
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
        bundledPlugins("org.jetbrains.plugins.terminal")
    }

    implementation(libs.javaWebSocket)
    implementation(libs.azureToolkitIdeCommonLib)

    implementation(libs.serializationJson)

    implementation(libs.ktorClientCore) {
        exclude(group = "org.jetbrains.kotlinx", module = "kotlinx-coroutines-core")
        exclude(group = "org.jetbrains.kotlinx", module = "kotlinx-coroutines-core-jvm")
        exclude(group = "org.jetbrains.kotlinx", module = "kotlinx-coroutines-jdk8")
        exclude(group = "org.jetbrains.kotlinx", module = "kotlinx-coroutines-slf4j")
    }
    implementation(libs.ktorClientCio) {
        exclude(group = "org.jetbrains.kotlinx", module = "kotlinx-coroutines-core")
        exclude(group = "org.jetbrains.kotlinx", module = "kotlinx-coroutines-jdk8")
    }
    implementation(libs.ktorClientContentNegotiation) {
        exclude(group = "org.jetbrains.kotlinx", module = "kotlinx-coroutines-core")
        exclude(group = "org.jetbrains.kotlinx", module = "kotlinx-coroutines-jdk8")
    }
    implementation(libs.ktorSerializationJson) {
        exclude(group = "org.jetbrains.kotlinx", module = "kotlinx-coroutines-core")
        exclude(group = "org.jetbrains.kotlinx", module = "kotlinx-coroutines-jdk8")
        exclude(group = "org.jetbrains.kotlinx", module = "kotlinx-serialization-json")
    }
    implementation(libs.ktorClientAuth) {
        exclude(group = "org.jetbrains.kotlinx", module = "kotlinx-coroutines-core")
        exclude(group = "org.jetbrains.kotlinx", module = "kotlinx-coroutines-jdk8")
    }
    implementation(libs.ktorClientWebsockets) {
        exclude(group = "org.jetbrains.kotlinx", module = "kotlinx-coroutines-core")
        exclude(group = "org.jetbrains.kotlinx", module = "kotlinx-coroutines-jdk8")
    }

    implementation(libs.coroutinesReactor) {
        exclude(group = "org.jetbrains.kotlinx", module = "kotlinx-coroutines-core")
    }
}
