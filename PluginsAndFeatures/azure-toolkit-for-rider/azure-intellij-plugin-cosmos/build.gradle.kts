plugins {
    id("java")
    id("org.jetbrains.intellij.platform.module")
    alias(libs.plugins.aspectj)
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
        bundledPlugin("com.intellij.database")
    }

    implementation(libs.azureToolkitLibs)
    implementation(libs.azureToolkitIdeLibs)
    implementation(libs.azureToolkitHdinsightLibs)

    implementation(project(path = ":azure-intellij-plugin-lib"))
    implementation(project(path = ":azure-intellij-resource-connector-lib"))
    implementation(libs.azureToolkitCosmosLib)
    implementation(libs.azureToolkitIdeCosmosLib)
    implementation(libs.azureToolkitIdentityLib)
    implementation(libs.azureToolkitIdeCommonLib)

    compileOnly(libs.lombok)
    compileOnly("org.jetbrains:annotations:24.0.0")
    annotationProcessor(libs.lombok)
    implementation(libs.azureToolkitCommonLib)
    aspect(libs.azureToolkitCommonLib)
    implementation("org.aspectj:aspectjrt:1.9.25")
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
